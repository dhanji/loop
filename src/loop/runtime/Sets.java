package loop.runtime;

import java.util.HashSet;
import java.util.Set;

/**
 * Runtime toolkit for sets.
 */
public class Sets {
  public static <T> Set<T> of() {
    return new HashSet<T>();
  }

  public static <T> Set<T> of(T...ts) {
    Set<T> set = new HashSet<T>(ts.length);
    for (int i = 0; i < ts.length; i++) {
      T t = ts[i];

      set.add(t);
    }

    return set;
  }
}
