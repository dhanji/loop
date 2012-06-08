package loop.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import loop.Loop;

/**
 * InvocationHandler for Java -> Loop communication
 */
public class LoopInvocationHandler implements InvocationHandler {

  private static HashMap<String, Class<?>> cache = new HashMap<String, Class<?>>();

  private Class<?> clazz;

  private String loopFile;

  public LoopInvocationHandler(Class<?> i) {
    String simpleName = i.getSimpleName();
    loopFile = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1) + ".loop";
    String path = i.getPackage().getName().replace(".", "/") + "/" + loopFile;
    if (cache.containsKey(path)) {
      this.clazz = cache.get(path);
    } else {
      this.clazz = Loop.compile(i.getClassLoader().getResource(path).getFile());
      cache.put(path, clazz);
    }
  }

  @Override
  public Object invoke(Object arg0, Method m, Object[] args) throws Throwable {
    Class<?>[] argsTypes;
    if (args != null) {
      argsTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        argsTypes[i] = Object.class;
      }
    } else {
      argsTypes = new Class<?>[0];
    }
    try {
      return clazz.getMethod(m.getName(), argsTypes).invoke(null, args);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(m.toGenericString() + " is not implemented on " + loopFile);
    } catch (Exception e) {
      throw e;
    }
  }
}
