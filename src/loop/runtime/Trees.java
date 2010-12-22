package loop.runtime;

import java.util.Map;
import java.util.TreeMap;

/**
 * Runtime binary tree data structure support for the Java side.
 */
public class Trees {
  public static <K, V> Map<K, V> of() {
    return new TreeMap<K, V>();
  }
  
  public static Map<Object, Object> of(Object...ts) {
    Map<Object, Object> map = new TreeMap<Object, Object>();
    for (int i = 0; i < ts.length; i += 2) {
      Object key = ts[i];
      Object value = ts[i + 1];

      map.put(key, value);
    }

    return map;
  }
}
