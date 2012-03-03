package loop.runtime;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Collections {

  public static Object obtain(Object collection, Integer from, Integer to) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(from, to);
    }

    throw new RuntimeException("Arrays not supported");
  }

  public static Object obtain(Object collection, Integer from) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.get(from);
    }

    throw new RuntimeException("Arrays not supported");
  }

  public static Object sliceFrom(Object collection, Integer from) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(from, list.size());
    }

    throw new RuntimeException("Arrays not supported");
  }

  public static Object sliceTo(Object collection, Integer to) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(0, to);
    }

    throw new RuntimeException("Arrays not supported");
  }
}
