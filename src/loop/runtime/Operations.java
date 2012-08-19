package loop.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Operations {

  @SuppressWarnings("unchecked")
  public static Object plus(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 + (Integer)arg1;
    } else if (arg0 instanceof String) {
      return ((String)arg0) + arg1;
    } else if (arg0 instanceof Collection) {
      Collection left = (Collection) arg0;
      Collection right = (Collection) arg1;
      List out = new ArrayList(left.size() + right.size());
      out.addAll(left);
      out.addAll(right);

      return out;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 + (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 + (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 + (Long)arg1;
    } else if (arg1 instanceof String) {
      return arg0.toString() + arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).add((BigInteger) arg1);
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).add((BigDecimal) arg1);
    } else if (arg0 == null)
      return arg1;

    throw new IllegalArgumentException("Cannot add objects of type "
        + (arg0 != null ? arg0.getClass() : arg0) + " and "
        + (arg1 != null ? arg1.getClass() : arg1));
  }

  @SuppressWarnings("unchecked")
  public static Object minus(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 - (Integer)arg1;
    } else if (arg0 instanceof Collection) {
      Collection left = (Collection) arg0;
      Collection right = (Collection) arg1;
      List out = new ArrayList(left.size() + right.size());
      out.addAll(left);
      out.removeAll(right);

      return out;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 - (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 - (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 - (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).subtract((BigInteger) arg1);
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).subtract((BigDecimal) arg1);
    }

    throw new IllegalArgumentException("Cannot subtract objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  @SuppressWarnings("unchecked")
  public static Object multiply(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 * (Integer)arg1;
    } else if (arg0 instanceof Collection) {
      Collection left = (Collection) arg0;
      int times = (Integer)arg1;
      List out = new ArrayList(left.size() * times);
      for (int i = 0; i < times; i++) {
        out.addAll(left);
      }

      return out;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 * (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 * (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 * (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).multiply((BigInteger) arg1);
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).multiply((BigDecimal) arg1);
    }

    throw new IllegalArgumentException("Cannot subtract objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Object divide(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 / (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 / (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 / (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 / (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).divide((BigInteger) arg1);
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).divide((BigDecimal) arg1);
    }

    throw new IllegalArgumentException("Cannot subtract objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Object remainder(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 % (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 % (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 % (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 % (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).remainder((BigInteger) arg1);
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).remainder((BigDecimal) arg1);
    }

    throw new IllegalArgumentException("Cannot subtract objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Boolean lesserThan(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 < (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 < (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 < (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 < (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).compareTo((BigInteger) arg1) == -1;
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).compareTo((BigDecimal) arg1) == -1;
    }

    throw new IllegalArgumentException("Cannot compare objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Boolean greaterThan(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 > (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 > (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 > (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 > (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).compareTo((BigInteger) arg1) == 1;
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).compareTo((BigDecimal) arg1) == 1;
    }

    throw new IllegalArgumentException("Cannot compare objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Boolean greaterThanOrEqual(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 >= (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 >= (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 >= (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 >= (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).compareTo((BigInteger) arg1) >= 0;
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).compareTo((BigDecimal) arg1) >= 0;
    }

    throw new IllegalArgumentException("Cannot compare objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Boolean lesserThanOrEqual(Object arg0, Object arg1) {
    if (arg0 instanceof Integer) {
      return (Integer)arg0 <= (Integer)arg1;
    } else if (arg0 instanceof Float) {
      return (Float)arg0 <= (Float)arg1;
    } else if (arg0 instanceof Double) {
      return (Double)arg0 <= (Double)arg1;
    } else if (arg0 instanceof Long) {
      return (Long)arg0 <= (Long)arg1;
    } else if (arg0 instanceof BigInteger) {
      return ((BigInteger)arg0).compareTo((BigInteger) arg1) <= 0;
    } else if (arg0 instanceof BigDecimal) {
      return ((BigDecimal)arg0).compareTo((BigDecimal) arg1) <= 0;
    }

    throw new IllegalArgumentException("Cannot compare objects of type " + arg0.getClass() + " and " + arg1.getClass());
  }

  public static Boolean equal(Object arg0, Object arg1) {
    if (arg0 == null) {
      return arg1 == null;
    }

    return arg0.equals(arg1);
  }

  public static Boolean notEqual(Object arg0, Object arg1) {
    if (arg0 == null) {
      return arg1 != null;
    }

    return !arg0.equals(arg1);
  }

  public static Boolean and(Object arg0, Object arg1) {
    return ((Boolean) arg0) && ((Boolean) arg1);
  }

  public static Boolean or(Object arg0, Object arg1) {
    return ((Boolean) arg0) || ((Boolean) arg1);
  }
}
