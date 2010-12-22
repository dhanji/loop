package loop.runtime;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime list data structure support for the Java side.
 */
public class Lists {
  public static <T> List<T> of() {
    return new ArrayList<T>();
  }
  
  public static <T> List<T> of(T...ts) {
    List<T> list = new ArrayList<T>(ts.length);
    for (int i = 0; i < ts.length; i++) {
      T t = ts[i];

      list.add(t);
    }

    return list;
  }

  public static List<Integer> of(int[] ts) {
    List<Integer> list = new ArrayList<Integer>(ts.length);
    for (int i = 0; i < ts.length; i++) {
      int t = ts[i];

      list.add(t);
    }

    return list;
  }

  public static int get(List<Integer> list, int index) {
    return list.get(index);
  }
}
