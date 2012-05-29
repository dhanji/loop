package loop.lang;

import loop.LoopExecutionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The root object of all object instances in loop. Actually this is the
 * Java class that backs all instances of all loop types.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopObject extends HashMap<Object, Object> {
  private static final String NO_DESTROY =
      "This ain't Javascript! Can't mutate objects destructively.";
  private final LoopClass type;

  public LoopObject(LoopClass type) { this.type = type; }

  public LoopClass getType() {
    return type;
  }

  @Override public Object remove(Object o) {
    throw new LoopExecutionException(NO_DESTROY);
  }

  @Override public void clear() {
    throw new LoopExecutionException(NO_DESTROY);
  }

  @Override public Set<Object> keySet() {
    return Collections.unmodifiableSet(super.keySet());
  }

  @Override public Collection<Object> values() {
    return Collections.unmodifiableCollection(super.values());
  }

  @Override public Set<Map.Entry<Object, Object>> entrySet() {
    return Collections.unmodifiableSet(super.entrySet());
  }

  public ImmutableLoopObject immutize() {
    return new ImmutableLoopObject(type, this);
  }

  public LoopObject copy() {
    return copy(new LoopObject(type), this);
  }

  @SuppressWarnings("unchecked")
  private static <T> T copy(Map<Object, Object> to, Map<Object, Object> from) {
    for (Map.Entry<Object, Object> entry : from.entrySet()) {
      Object value = entry.getValue();

      // Make mutable if necessary.
      if (value instanceof Collection)
        value = copy((Collection) value);
      else if (value instanceof Map) {
        Map toCopy = (Map) value;
        value = copy(new HashMap<Object, Object>(toCopy.size()), toCopy);
      }

      to.put(entry.getKey(), value);
    }

    return (T) to;
  }

  @SuppressWarnings("unchecked")
  public static Collection copy(Collection value) {
    Collection<Object> copy;
    if (value instanceof Set) {
      copy = new HashSet<Object>(value.size());
    } else {
      copy = new ArrayList<Object>(value.size());
    }

    for (Object item : value) {
      if (item instanceof Collection)
        item = copy((Collection) item);
      else if (item instanceof Map) {
        Map<Object, Object> toCopy = (Map<Object, Object>) item;
        item = copy(new HashMap<Object, Object>(toCopy.size()), toCopy);
      }

      copy.add(item);
    }

    return copy;
  }
}
