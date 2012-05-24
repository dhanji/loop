package loop;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopClassLoader extends ClassLoader {
  final ConcurrentMap<String, byte[]> rawClasses = new ConcurrentHashMap<String, byte[]>();
  final ConcurrentMap<String, Class<?>> loaded = new ConcurrentHashMap<String, Class<?>>();
  public static volatile LoopClassLoader CLASS_LOADER = new LoopClassLoader();

  public void put(String javaClass, byte[] bytes) {
    if (null != rawClasses.putIfAbsent(javaClass, bytes))
      throw new RuntimeException("Illegal attempt to define duplicate class");
  }

  public boolean isLoaded(String javaClass) {
    return loaded.containsKey(javaClass);
  }

  @Override
  protected Class findClass(String name) throws ClassNotFoundException {
    Class<?> clazz = loaded.get(name);
    if (null != clazz)
      return clazz;

    final byte[] bytes = rawClasses.remove(name);
    if (bytes != null) {

      // We don't define loop classes in the parent class loader.
      clazz = defineClass(name, bytes);

      if (loaded.putIfAbsent(name, clazz) != null)
        throw new RuntimeException("Attempted duplicate class definition for " + name);
      return clazz;
    }
    return super.findClass(name);
  }

  public Class defineClass(String name, byte[] b) {
    return defineClass(name, b, 0, b.length);
  }

  public static void reset() {
    CLASS_LOADER = new LoopClassLoader();
    Thread.currentThread().setContextClassLoader(CLASS_LOADER);
  }
}
