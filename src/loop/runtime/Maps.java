package loop.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime hashmap data structure support for the Java side.
 */
public class Maps {
  public static <K, V> Map<K, V> of() {
    return new HashMap<K, V>();
  }
  
  public static Map<Object, Object> of(Object...ts) {
    Map<Object, Object> map = new HashMap<Object, Object>((int)(ts.length * 1.5));
    for (int i = 0; i < ts.length; i += 2) {
      Object key = ts[i];
      Object value = ts[i + 1];

      map.put(key, value);
    }

    return map;
  }
}
