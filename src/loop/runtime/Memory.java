package loop.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Support class for software transactional memory and global immutable
 * shared state.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Memory {
  public static Map<Object, Object> unsafe = new HashMap<Object, Object>();

  private static final ConcurrentMap<String, Object> cells = new ConcurrentHashMap<String, Object>();

  public static Object insert(Object key, Object value) {
    assert key instanceof String;

    return cells.put((String) key, value);
  }

  public static Object update(Object key, Object old, Object value) {
    assert key instanceof String;

    return cells.replace((String) key, old, value);
  }

  public static Object delete(Object key) {
    assert key instanceof String;

    return cells.remove(key);
  }

  public static Object lookupOrInsert(Object key, Object value) {
    assert key instanceof String;

    Object previous = cells.putIfAbsent((String) key, value);
    return previous == null ? value : previous;
  }

  public static Object lookup(Object key) {
    assert key instanceof String;

    return cells.get(key);
  }
}
