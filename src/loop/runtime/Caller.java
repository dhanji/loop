package loop.runtime;

import loop.LoopClassLoader;

import java.lang.reflect.*;
import java.util.*;
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
  private static final Map<Class, Class> convertibleTypes = new HashMap<Class, Class>();

  static {
    compatibleTypes.put(long.class, new HashSet<Class>(
        Arrays.asList(int.class, Short.class, short.class, Integer.class, Long.class, Byte.class,
            byte.class)));
    compatibleTypes.put(int.class, new HashSet<Class>(
        Arrays.asList(short.class, Short.class, Integer.class, Byte.class, byte.class)));

    convertibleTypes.put(int.class, Number.class);
    convertibleTypes.put(long.class, Number.class);
    convertibleTypes.put(double.class, Double.class);
    convertibleTypes.put(boolean.class, Boolean.class);
    convertibleTypes.put(short.class, Short.class);
    convertibleTypes.put(float.class, Float.class);
    convertibleTypes.put(byte.class, Byte.class);
    convertibleTypes.put(char.class, Character.class);
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

  public static Object call(Object target, String method) throws Throwable {
    return call(target, method, EMPTY_ARRAY);
  }

  public static void print(Object thing) {
    System.out.println(thing);
  }

  public static void print(int thing) {
    System.out.println(thing);
  }


  @SuppressWarnings("unchecked")
  public static Object instantiate(String type, Object... args) throws Exception {
    Class<?> clazz = Class.forName(type);

    String key = type + ':' + args.length;
    Constructor ctor = staticConstructorCache.get(key);

    if (null == ctor) {
      boolean cache = true;

      Constructor ctor1 = null;
      // Choose the appropriate constructor.
      for (Constructor constructor : clazz.getConstructors()) {
        if (constructor.getParameterTypes().length == args.length) {
          Class[] parameterTypes = constructor.getParameterTypes();

          // Don't cache constructors if there are other ctors of the same length, as
          // they need to be resolved each time.
          if (ctor1 != null && ctor1.getParameterTypes().length == parameterTypes.length)
            cache = false;

          boolean acceptable = true;
          for (int i = 0, parameterTypesLength = parameterTypes.length;
               i < parameterTypesLength;
               i++) {
            Class argType = parameterTypes[i];
            Object arg = args[i];

            if (arg != null && !isAssignable(argType, arg)) {
              acceptable = false;
              break;
            }
          }

          if (acceptable) {
            ctor1 = constructor;
          }
        }
      }

      if (ctor1 != null && cache)
        staticConstructorCache.putIfAbsent(key, ctor1);
      ctor = ctor1;
    }

    if (ctor == null)
      throw new RuntimeException("No suitable constructor matched");

    return ctor.newInstance(args);
  }

  private static boolean isAssignable(Class argType, Object arg) {
    Set<Class> compatible = compatibleTypes.get(argType);
    if (compatible != null) {
      return compatible.contains(arg.getClass());
    }

    return (Number.class.isAssignableFrom(argType) && Number.class.isAssignableFrom(arg.getClass()))
        || argType.isAssignableFrom(arg.getClass());
  }

  // Messy, we should inline this really.
  public static Object range(Object from, Object to) {
    if (from instanceof Integer) {
      Integer start = (Integer) from;
      int distance = ((Integer) to) - start + 1;
      List<Integer> range = new ArrayList<Integer>(distance);
      for (int i = start; i < start + distance; i++) {
        range.add(i);
      }
      return range;
    }

    throw new RuntimeException("Unknown range type: " + from + " - " + to);
  }

  public static Object dereference(Object target, String property) throws Throwable {
    boolean isMap = target instanceof Map;
    if (isMap) {
      Object value = ((Map) target).get(property);
      if (value != null)
        return value;
    }

    // This key can be improved to use a bitvector, for example.
    String name = target.getClass().getName();
    String key = new StringBuilder(name.length() + property.length() + 5).append(name)
        .append(':')
        .append(property)
        .append(":0")
        .toString();

    Method toCall = dynamicMethodCache.get(key);

    // Now search getters instead.
    if (toCall == null) {
      String altMethod;
      if (property.length() == 1)
        altMethod = "get" + property.toUpperCase();
      else
        altMethod = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);

      for (Method candidate : target.getClass().getMethods()) {
        if (candidate.getName().equals(altMethod) && candidate.getParameterTypes().length == 0) {
          toCall = candidate;
          break;
        }
      }

      if (null == toCall) {
        if (isMap)
          return null;
        throw new RuntimeException("Property getter not found: " + name + "#" + property);
      }

      if (!toCall.isAccessible())
        toCall.setAccessible(true);

      dynamicMethodCache.putIfAbsent(key, toCall);
    }

    try {
      return toCall.invoke(target);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  public static Object call(Object target, String method, Object... args) throws Throwable {
    if (target == null)
      return null;

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
        if (signatureMatches(method, candidate, args)) {
          toCall = candidate;
          break;
        }
      }

      if (toCall == null) {
        for (Method candidate : target.getClass().getDeclaredMethods()) {
          if (signatureMatches(method, candidate, args)) {
            toCall = candidate;
            break;
          }
        }
      }

      if (null == toCall) {
        Set<Method> methods = bestMatches(target.getClass().getMethods(), method);
        methods.addAll(bestMatches(target.getClass().getDeclaredMethods(), method));

        throw new RuntimeException("Method not found: " + name + "#" + method
            + "(" + Arrays.toString(args) + ")"
            + (methods.isEmpty() ? "" :
            "\nBest matches in " + name + "\n\n" + toStringList(methods)));
      }

      if (!toCall.isAccessible())
        toCall.setAccessible(true);

      dynamicMethodCache.putIfAbsent(key, toCall);
    }

    try {
      return toCall.invoke(target, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static boolean signatureMatches(String method, Method candidate, Object[] args) {
    Class<?>[] parameterTypes = candidate.getParameterTypes();
    if (!(candidate.getName().equals(method) && parameterTypes.length == args.length))
      return false;

    for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
      Class<?> parameterType = parameterTypes[i];
      Object arg = args[i];

      if (arg != null) {
        Class<?> argClass = arg.getClass();
        if (parameterType.isPrimitive())
          parameterType = convertibleTypes.get(parameterType);
        if (!parameterType.isAssignableFrom(argClass))
          return false;
      }
    }
    return true;
  }

  private static Set<Method> bestMatches(Method[] candidates, String method) {
    Set<Method> matches = new HashSet<Method>();

    for (Method candidate : candidates) {
      if (candidate.getName().equalsIgnoreCase(method)) {
        matches.add(candidate);
      }
    }

    return matches;
  }

  private static String toStringList(Collection<Method> methods) {
    StringBuilder out = new StringBuilder();
    for (Method method : methods) {
        out.append("    ").append(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
          Class<?> type = parameterTypes[i];
          out.append(type.getSimpleName());

          if (i < parameterTypes.length - 1)
            out.append(", ");
        }

        out.append(")\n");
    }

    return out.toString();
  }

  public static Object callStatic(String target, String method) throws Throwable {
    return callStatic(target, method, EMPTY_ARRAY);
  }

  public static Object callStatic(Class<?> clazz, String method) throws Throwable {
    return callStatic(clazz, method, EMPTY_ARRAY);
  }

  public static Object callClosure(Closure closure, String target) throws Throwable {
    return callStatic(target, closure.name, closure.freeVariables);
  }

  public static Object callClosure(Closure closure, String target, Object[] args) throws Throwable {
    int argLen = args.length;
    int freeVarLen = closure.freeVariables.length;
    if (freeVarLen > 0) {
      Object[] combinedArgs = new Object[argLen + freeVarLen];
      System.arraycopy(args, 0, combinedArgs, 0, argLen);
      System.arraycopy(closure.freeVariables, 0, combinedArgs, argLen, freeVarLen);

      args = combinedArgs;
    }
    return callStatic(closure.target, closure.name, args);
  }

  public static Object callStatic(String target, String method, Object[] args) throws Throwable {
    return callStatic(Class.forName(target, true, LoopClassLoader.CLASS_LOADER), method, args);
  }

  public static Object callStatic(Class<?> clazz, String method, Object[] args) throws Throwable {
    Method toCall;

    String target = clazz.getName();
    final String key = target + ':' + method + ':' + args.length;
    toCall = staticMethodCache.get(key);

    if (toCall == null) {
      for (Method candidate : clazz.getMethods()) {
        if (signatureMatches(method, candidate, args)) {
          toCall = candidate;
          break;
        }
      }

      if (toCall == null) {
        for (Method candidate : clazz.getDeclaredMethods()) {
          if (signatureMatches(method, candidate, args)) {
            toCall = candidate;
            break;
          }
        }
      }

      if (toCall == null) {
        Set<Method> methods = bestMatches(clazz.getMethods(), method);
        methods.addAll(bestMatches(clazz.getDeclaredMethods(), method));

        throw new RuntimeException(
            "Function not found: " + target + "#" + method + "(" + Arrays.toString(args) + ")"
                + (methods.isEmpty() ? "" :
                "\nBest matches in " + target + "\n\n" + toStringList(methods)));
      }

      if (!toCall.isAccessible())
        toCall.setAccessible(true);

      staticMethodCache.put(key, toCall);
    }

    try {
      return toCall.invoke(target, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  public static Object getStatic(String target, String field) throws Exception {
    return getStatic(Class.forName(target, true, LoopClassLoader.CLASS_LOADER), field);
  }

  public static Object getStatic(Class<?> clazz, String field) throws Exception {
    Field toCall;

    final String key = clazz.getName() + field;
    toCall = staticFieldCache.get(key);

    if (toCall == null) {
      if ("class".equals(field))
        return clazz;

      for (Field candidate : clazz.getDeclaredFields()) {
        if (Modifier.isStatic(candidate.getModifiers()) && candidate.getName().equals(field)) {
          toCall = candidate;
          break;
        }
      }

      if (toCall == null)
        throw new RuntimeException(
            "No such method could be resolved: " + field + " on type " + clazz.getName());

      staticFieldCache.put(key, toCall);
    }

    return toCall.get(null);
  }

  public static void raise(Object message) {
    if (message instanceof String)
      throw new RuntimeException(message.toString());

    throw new RuntimeException((RuntimeException) message);
  }
}
