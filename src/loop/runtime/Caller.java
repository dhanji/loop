package loop.runtime;

import loop.LoopClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflective method caller.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Caller {
  public static Object add(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 + (Integer)arg1;
    } else if (arg0 instanceof List) {
      List left = (List) arg0;
      List right = (List) arg1;
      List out = new ArrayList(left.size() + right.size());
      out.addAll(left);
      out.addAll(right);

      return out;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 + (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 + (Long)arg1;
    }

    throw new IllegalArgumentException("Cannot add objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Object call(Object target, String method) {
    return call(target, method, new Object[0]);
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
    Method toCall = null;
    try {
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
