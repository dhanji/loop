package loop.runtime;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Collections {

  public static Object obtain(Object collection, Integer from, Integer to) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(from, to + 1);
    } else if (collection instanceof String) {
      String string = (String) collection;

      return string.substring(from, to + 1);
    } else if (collection instanceof Object[]) {
      Object[] array = (Object[]) collection;

      return Arrays.copyOfRange(array, from, to + 1);
    }

    throw new RuntimeException("Collection type: "
        + (collection != null ? collection.getClass() : "null")
        + " not supported");
  }

  public static Object obtain(Object collection, Object exactly) {
    if (collection instanceof List) {
      List list = (List) collection;

      return list.get((Integer) exactly);
    } else if (collection instanceof String) {
      String string = (String) collection;

      if (exactly instanceof Integer)
        return Character.toString(string.charAt((Integer) exactly));
      else if (exactly instanceof String)
        return string.indexOf(exactly.toString());
    } else if (collection instanceof Map) {
      Map map = (Map) collection;

      return map.get(exactly);
    } else if (collection instanceof Object[]) {
      Object[] array = (Object[]) collection;

      return array[(Integer) exactly];
    }

    throw new RuntimeException("Collection type: "
        + (collection != null ? collection.getClass() : "null")
        + " not supported");
  }

  @SuppressWarnings("unchecked")
  public static Object store(Object collection, Object property, Object value) throws Throwable {
    if (collection instanceof List) {
      List list = (List) collection;
      list.set((Integer) property, value);

    } else if (collection instanceof Map) {
      Map map = (Map) collection;
      map.put(property, value);

    } else if (collection instanceof Object[]) {
      @SuppressWarnings("MismatchedReadAndWriteOfArray")  // Incorrect inspection.
      Object[] array = (Object[]) collection;

      //noinspection RedundantCast
      array[(Integer) property] = value;
    } else {
      // Set value.
      String prop = property.toString();
      Caller.call(collection, "set" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1),
          value);
    }

    return collection;
  }

  public static Object sliceFrom(Object collection, Object fromObj) {
    int from = (Integer)fromObj;
    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(from, list.size());
    } else if (collection instanceof String) {
      String string = (String) collection;

      return string.substring(from, string.length());
    } else if (collection instanceof Object[]) {
      Object[] array = (Object[]) collection;

      return Arrays.copyOfRange(array, from, array.length);
    }

    throw new RuntimeException("Collection type: "
        + (collection != null ? collection.getClass() : "null")
        + " not supported");
  }

  public static Object sliceTo(Object collection, Object toObj) {
    int to = (Integer)toObj;

    if (collection instanceof List) {
      List list = (List) collection;

      return list.subList(0, to + 1);
    } else if (collection instanceof String) {
      String string = (String) collection;

      return string.substring(0, to + 1);
    } else if (collection instanceof Object[]) {
      Object[] array = (Object[]) collection;

      return Arrays.copyOfRange(array, 0, to + 1);
    }

    throw new RuntimeException("Collection type: "
        + (collection != null ? collection.getClass() : "null")
        + " not supported");
  }
}
