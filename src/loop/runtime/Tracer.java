package loop.runtime;

import java.util.Stack;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Tracer {
  private static final ThreadLocal<Stack<String>> tracingStack = new ThreadLocal<Stack<String>>();

  public static void push(String element) {
    Stack<String> trace = tracingStack.get();
    if (trace == null) {
      tracingStack.set(trace = new Stack<String>());
    }

    trace.push(element);
  }

  public static void pop() {
    tracingStack.get().pop();
  }

  public static Stack<String> getStackTrace() {
    return tracingStack.get();
  }

  public static void complete() {
    tracingStack.remove();
  }
}
