package loop.runtime;

import loop.LoopClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reflective method caller.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Caller {
  public static volatile ConcurrentMap<String, Method> staticMethodCache =
      new ConcurrentHashMap<String, Method>();

  public static Object call(Object target, String method) {
    return call(target, method, new Object[0]);
  }

  public static void print(Object thing) {
    System.out.println(thing);
  }

  @SuppressWarnings("unchecked")
  public static Object instantiate(String type, Object... args) {
    try {
      Class<?> clazz = Class.forName(type);

      // Choose the appropriate constructor.
      Constructor ctor = null;
      for (Constructor constructor : clazz.getConstructors()) {
        if (constructor.getParameterTypes().length == args.length) {
          Class[] parameterTypes = constructor.getParameterTypes();

          boolean acceptable = true;
          for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
            Class argType = parameterTypes[i];
            Object arg = args[i];

            if (arg != null && !argType.isAssignableFrom(arg.getClass())) {
              acceptable = false;
            }
          }

          if (acceptable)
            ctor = constructor;
        }
      }

      if (ctor == null)
        throw new RuntimeException("No suitable constructor matched");

      return ctor.newInstance(args);

    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

  }

  public static Object call(Object target, String method, Object... args) {
    Method toCall = null;
    for (Method candidate : target.getClass().getMethods()) {
      if (candidate.getName().equals(method)) {
        toCall = candidate;
        break;
      }
    }

    if (toCall == null) {
      for (Method candidate : target.getClass().getDeclaredMethods()) {
        if (candidate.getName().equals(method)) {
          toCall = candidate;
          break;
        }
      }
    }

    try {
      return toCall.invoke(target, args);
    } catch (NullPointerException e) {
      throw new RuntimeException("No such method could be resolved: " + method + " on type " + target
          .getClass());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getCause());
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public static Object callStatic(String target, String method, Object... args) {
    Method toCall;

    try {
      final String key = target + method;
      toCall = staticMethodCache.get(key);

      if (toCall == null) {
        final Class<?> clazz = Class.forName(target, true, LoopClassLoader.CLASS_LOADER);
        for (Method candidate : clazz.getMethods()) {
          if (candidate.getName().equals(method)) {
            toCall = candidate;
            break;
          }
        }

        if (toCall == null) {
          for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName().equals(method)) {
              toCall = candidate;
              break;
            }
          }
        }

        staticMethodCache.put(key, toCall);
      }

      return toCall.invoke(target, args);
    } catch (NullPointerException e) {
      throw new RuntimeException("No such method could be resolved: " + method + " on type " + target);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getCause());
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
