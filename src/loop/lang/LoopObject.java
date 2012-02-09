package loop.lang;

import loop.LoopExecutionException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
}
