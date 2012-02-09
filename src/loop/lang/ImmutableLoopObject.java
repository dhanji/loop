package loop.lang;

import loop.LoopExecutionException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * The root object of all immutable object instances in loop. Actually this is the
 * Java class that backs all instances of immutable loop types.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ImmutableLoopObject extends LoopObject {
  private static final String IMMUTABILITY_ERROR =
      "Illegal attempt to create an object oriented language!";

  public ImmutableLoopObject(LoopClass type) {
    super(type);
  }

  @Override public Object put(Object o, Object o1) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public void putAll(Map<? extends Object, ? extends Object> map) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public void clear() {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public Object remove(Object o) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
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
