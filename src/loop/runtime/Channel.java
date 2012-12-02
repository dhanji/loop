package loop.runtime;

import loop.StackTraceSanitizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concurrent Channels support class for loop's event-driven channel API.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Channel {
  private static final ExecutorService GLOBAL_EXECUTOR = Executors.newCachedThreadPool();
  private static final int YIELD_FAIRNESS_CYCLES = 15;

  private static final String SHUTDOWN = "shutdown";
  private static final String DIE = "die";

  static {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override public void run() {
        GLOBAL_EXECUTOR.shutdown();
      }
    });
  }

  private final String name;
  private final Closure actor;
  private final ConcurrentLinkedQueue<Object> queue;
  private final AtomicBoolean running = new AtomicBoolean();
  private final Runnable runnable;
  private final ExecutorService executor;
  private final boolean isDedicatedPool;
  private final Map<String, Object> channelMemory = new HashMap<String, Object>();

  public Channel(String name, Closure actor, boolean parallel, int workers) {
    this.name = name;
    this.actor = actor;
    this.queue = new ConcurrentLinkedQueue<Object>();

    if (isDedicatedPool = workers > 0) {
      this.executor = Executors.newFixedThreadPool(workers);
    } else
      this.executor = GLOBAL_EXECUTOR;

    this.runnable = parallel ? concurrentRunnable : isolatedRunnable;
  }

  /**
   * Isolated runnables allow only one message to be processed at a time, regardless
   * of the available worker threads. They are useful for sharding semantics.
   */
  private final Runnable isolatedRunnable = new Runnable() {
    @Override public void run() {
      if (!running.compareAndSet(false, true))
        return;

      try {
        currentChannelMemory.set(channelMemory);

        int processed = 0;
        while (!queue.isEmpty() && processed < YIELD_FAIRNESS_CYCLES) {
          try {
            Object result = Caller.callClosure(actor, actor.target, new Object[]{queue.poll()});

            // Check if we should shutdown this channel.
            // Allows graceful drain of queued messages.
            if (SHUTDOWN.equals(result))
              shutdown();
            else if (DIE.equals(result)) {
              // Process no more messages.
              die();
              break;
            }

          } catch (Throwable throwable) {
            try {
              StackTraceSanitizer.clean(throwable);

              // Swallow exception if possible.
              throwable.printStackTrace(System.err);
            } finally {
              // Quit VM forcibly on out of memory error.
              if (throwable instanceof OutOfMemoryError)
                System.exit(1);
            }
          } finally {
            processed++;
          }
        }
      } finally {
        currentChannelMemory.remove();
        running.compareAndSet(true, false);

        // Tail-call ourselves if we're not done with this queue.
        if (!queue.isEmpty())
          executor.submit(isolatedRunnable);
      }
    }
  };

  /**
   * Concurrent runnables burst-process tasks as fast as possible. This is
   * the default.
   */
  private final Runnable concurrentRunnable = new Runnable() {
    @Override public void run() {
      int processed = 0;
      while (!queue.isEmpty() && processed < YIELD_FAIRNESS_CYCLES) {
        try {
          Object result = Caller.callClosure(actor, actor.target, new Object[]{queue.poll()});

          if (SHUTDOWN.equals(result))
            shutdown();
          else if (DIE.equals(result)) {
            // Process no more messages.
            die();
            break;
          }
        } catch (Throwable throwable) {
          try {
            StackTraceSanitizer.clean(throwable);

            // Swallow exception if possible.
            throwable.printStackTrace(System.err);
          } finally {
            // Quit VM forcibly on out of memory error.
            if (throwable instanceof OutOfMemoryError)
              System.exit(1);
          }
        } finally {
          processed++;
        }
      }
    }
  };

  private static final ThreadLocal<Map<String, Object>> currentChannelMemory = new ThreadLocal<Map<String, Object>>();

  public static Object currentMemory() {
    Map<String, Object> map = currentChannelMemory.get();
    if (map == null)
      throw new RuntimeException("Illegal shared memory request. (Hint: use transactional cells instead)");
    return map;
  }

  public void shutdown() {
    channels.remove(name);

    if (isDedicatedPool)
      executor.shutdown();
  }

  public void die() {
    channels.remove(name);
    queue.clear();

    if (isDedicatedPool)
      executor.shutdownNow();
  }

  public void receive(Object message) {
    queue.add(message);

    if (!running.get())
      executor.submit(runnable);
  }

  private static final ConcurrentMap<String, Channel> channels =
      new ConcurrentHashMap<String, Channel>();

  public static Channel named(Object name) {
    assert name instanceof String;
    Channel channel = channels.get(name);
    if (channel == null)
      throw new RuntimeException("No such channel established: " + name);

    return channel;
  }

  public static void establish(Object nameObj, Object actor, Object optionsObj) {
    assert nameObj instanceof String;
    assert actor instanceof Closure;
    assert optionsObj instanceof Map;

    String name = (String) nameObj;
    @SuppressWarnings("unchecked")
    Map<String, Object> options = (Map<String, Object>) optionsObj;

    Object serialize = options.get("serialize");
    Object threads = options.get("workers");

    int workers = 0;
    if (null != threads)
      workers = (Integer)threads;
    boolean parallel = serialize == null || !(Boolean) serialize;

    channels.put(name, new Channel(name, (Closure)actor, parallel, workers));
  }
}
