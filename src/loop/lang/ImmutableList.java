package loop.lang;

import loop.LoopExecutionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ImmutableList extends ArrayList implements Immutable {

  public ImmutableList(Collection<?> collection) {
    IdentityHashMap<Object, Object> cyclesCheck = new IdentityHashMap<Object, Object>();
    cyclesCheck.put(collection, this);

    deepCopy(collection, cyclesCheck);
  }

  public ImmutableList(Collection<?> collection, IdentityHashMap<Object, Object> cyclesCheck) {
    deepCopy(collection, cyclesCheck);
  }

  @SuppressWarnings("unchecked")
  private void deepCopy(Collection<?> collection, IdentityHashMap<Object, Object> cyclesCheck) {
    for (Object value : collection) {

      // Make immutable copy of value if necessary.
      if (value instanceof Map) {
        Object previouslyCopied = cyclesCheck.get(value);
        if (previouslyCopied != null)
          value = previouslyCopied;
        else {
          ImmutableLoopObject copied =
              new ImmutableLoopObject(LoopClass.IMMUTABLE_MAP, (Map<Object, Object>) value, cyclesCheck);
          cyclesCheck.put(value, copied);
          value = copied;
        }
      } else if (value instanceof Collection) {
        Object previouslyCopied = cyclesCheck.get(value);

        if (previouslyCopied != null)
          value = previouslyCopied;
        else {
          ImmutableList copied = new ImmutableList((Collection<?>) value, cyclesCheck);
          cyclesCheck.put(value, copied);
          value = copied;
        }
      }

      // Ensure immutability.
      if (!ImmutableLoopObject.isImmutable(value))
        throw new LoopExecutionException("Cannot add a mutable value to an immutable object");

      super.add(value);
    }
  }

  @Override public Object set(int i, Object o) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public boolean add(Object o) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public void add(int i, Object o) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public Object remove(int i) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public boolean remove(Object o) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public void clear() {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public boolean addAll(Collection collection) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public boolean addAll(int i, Collection collection) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override protected void removeRange(int i, int i1) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }

  @Override public boolean removeAll(Collection objects) {
    throw new LoopExecutionException(ImmutableLoopObject.IMMUTABILITY_ERROR);
  }
}
