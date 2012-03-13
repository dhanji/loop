package loop.runtime;

import loop.LoopClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reflective method caller.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings("unchecked")
public class Caller {
  private static final Map<Class, Set<Class>> compatibleTypes = new HashMap<Class, Set<Class>>();
  static {
    compatibleTypes.put(long.class, new HashSet<Class>(
        Arrays.asList(int.class, Short.class, short.class, Integer.class, Long.class, Byte.class,
            byte.class)));
    compatibleTypes.put(int.class, new HashSet<Class>(
        Arrays.asList(short.class, Short.class, Integer.class, Byte.class, byte.class)));
  }

  // Caches for high performance.
  private static volatile ConcurrentMap<String, Method> staticMethodCache =
      new ConcurrentHashMap<String, Method>();
  private static volatile ConcurrentMap<String, Constructor> staticConstructorCache =
      new ConcurrentHashMap<String, Constructor>();
  private static volatile ConcurrentMap<String, Field> staticFieldCache =
      new ConcurrentHashMap<String, Field>();
  private static volatile ConcurrentMap<String, Method> dynamicMethodCache =
      new ConcurrentHashMap<String, Method>();
  public static final Object[] EMPTY_ARRAY = new Object[0];

  public static void reset() {
    staticConstructorCache = new ConcurrentHashMap<String, Constructor>();
    staticMethodCache = new ConcurrentHashMap<String, Method>();
    dynamicMethodCache = new ConcurrentHashMap<String, Method>();
    staticFieldCache = new ConcurrentHashMap<String, Field>();
  }

  public static Object call(Object target, String method) {
    return call(target, method, EMPTY_ARRAY);
  }

  public static void print(Object thing) {
    System.out.println(thing);
  }

  public static void print(int thing) {
    System.out.println(thing);
  }


  @SuppressWarnings("unchecked")
  public static Object instantiate(String type, Object... args) {
    try {
      Class<?> clazz = Class.forName(type);

      String key = type + ':' + args.length;
      Constructor ctor = staticConstructorCache.get(key);

      if (null == ctor) {
        boolean cache = true;

        // Choose the appropriate constructor.
        for (Constructor constructor : clazz.getConstructors()) {
          if (constructor.getParameterTypes().length == args.length) {
            Class[] parameterTypes = constructor.getParameterTypes();

            // Don't cache constructors if there are other ctors of the same length, as
            // they need to be resolved each time.
            if (ctor != null && ctor.getParameterTypes().length == parameterTypes.length)
              cache = false;

            boolean acceptable = true;
            for (int i = 0, parameterTypesLength = parameterTypes.length;
                 i < parameterTypesLength;
                 i++) {
              Class argType = parameterTypes[i];
              Object arg = args[i];

              if (arg != null && !isAssignable(argType, arg)) {
                acceptable = false;
              }
            }

            if (acceptable) {
              ctor = constructor;
            }
          }
        }

        if (ctor != null && cache)
          staticConstructorCache.putIfAbsent(key, ctor);
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

  private static boolean isAssignable(Class argType, Object arg) {
    Set<Class> compatible = compatibleTypes.get(argType);
    if (compatible != null) {
      return compatible.contains(arg.getClass());
    }

    return (Number.class.isAssignableFrom(argType) && Number.class.isAssignableFrom(arg.getClass()))
        || argType.isAssignableFrom(arg.getClass());
  }

  public static Object call(Object target, String method, Object... args) {
    if (target == null)
      return null;

    if (target instanceof Map)
      return ((Map)target).get(method);

    // This key can be improved to use a bitvector, for example.
    String name = target.getClass().getName();
    String key = new StringBuilder(name.length() + method.length() + 5).append(name)
        .append(':')
        .append(method)
        .append(':')
        .append(args.length)
        .toString();

    Method toCall = dynamicMethodCache.get(key);

    if (toCall == null) {
      for (Method candidate : target.getClass().getMethods()) {
        if (candidate.getName().equals(method) && candidate.getParameterTypes().length == args.length) {
          toCall = candidate;
          break;
        }
      }

      // Now search getters instead.
      String altMethod = "get" + Character.toUpperCase(method.charAt(0)) + method.substring(1);
      for (Method candidate : target.getClass().getMethods()) {
        if (candidate.getName().equals(altMethod) && candidate.getParameterTypes().length == args.length) {
          toCall = candidate;
          break;
        }
      }

      if (toCall == null) {
        for (Method candidate : target.getClass().getDeclaredMethods()) {
          if (candidate.getName().equals(method) && candidate.getParameterTypes().length == args.length) {
            toCall = candidate;
            break;
          }
        }
      }

      dynamicMethodCache.putIfAbsent(key, toCall);
    }

    try {
      return toCall.invoke(target, args);
    } catch (NullPointerException e) {
      throw new RuntimeException(
          "No such method could be resolved: " + method + " on type " + target
              .getClass());
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getCause());
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public static Object callStatic(String target, String method) {
    return callStatic(target, method, EMPTY_ARRAY);
  }

  public static Object callClosure(Closure closure, String target) {
    return callStatic(target, closure.name, closure.freeVariables);
  }

  public static Object callClosure(Closure closure, String target, Object[] args) {
    int argLen = args.length;
    int freeVarLen = closure.freeVariables.length;
    if (freeVarLen > 0) {
      Object[] combinedArgs = new Object[argLen + freeVarLen];
      System.arraycopy(args, 0, combinedArgs, 0, argLen);
      System.arraycopy(closure.freeVariables, 0, combinedArgs, argLen, freeVarLen);

      args = combinedArgs;
    }
    return callStatic(target, closure.name, args);
  }

  public static Object callStatic(String target, String method, Object[] args) {
    Method toCall;

    try {
      final String key = target + ':' + method;
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

        if (!toCall.isAccessible())
          toCall.setAccessible(true);

        staticMethodCache.put(key, toCall);
      }

      return toCall.invoke(target, args);
    } catch (NullPointerException e) {
      throw new RuntimeException(
          "No such method could be resolved: " + method + " on type " + target);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getCause());
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object getStatic(String target, String field) {
    Field toCall;

    try {
      final String key = target + field;
      toCall = staticFieldCache.get(key);

      if (toCall == null) {
        final Class<?> clazz = Class.forName(target, true, LoopClassLoader.CLASS_LOADER);
        if (toCall == null) {
          for (Field candidate : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(candidate.getModifiers()) && candidate.getName().equals(field)) {
              toCall = candidate;
              break;
            }
          }
        }

        staticFieldCache.put(key, toCall);
      }

      return toCall.get(null);
    } catch (NullPointerException e) {
      throw new RuntimeException(
          "No such method could be resolved: " + field + " on type " + target);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e.getCause());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
