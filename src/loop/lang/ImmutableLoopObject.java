package loop.lang;

import loop.LoopExecutionException;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The root object of all immutable object instances in loop. Actually this is the
 * Java class that backs all instances of immutable loop types.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ImmutableLoopObject extends LoopObject implements Immutable {
  static final String IMMUTABILITY_ERROR =
      "Illegal attempt to create an object oriented language!";

  public ImmutableLoopObject(LoopClass type, Map<Object, Object> source) {
    super(type);

    IdentityHashMap<Object, Object> cyclesCheck = new IdentityHashMap<Object, Object>();
    cyclesCheck.put(source, this);

    deepCopy(cyclesCheck, source);
  }

  public ImmutableLoopObject(LoopClass type,
                             Map<Object, Object> source,
                             IdentityHashMap<Object, Object> cyclesCheck) {
    super(type);
    deepCopy(cyclesCheck, source);
  }

  @SuppressWarnings("unchecked")
  private void deepCopy(IdentityHashMap<Object, Object> cyclesCheck, Map<Object, Object> source) {
    for (Map.Entry<Object, Object> entry : source.entrySet()) {
      Object value = entry.getValue();

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
      if (!isImmutable(value))
        throw new LoopExecutionException("Cannot add a mutable value to an immutable object");

      super.put(entry.getKey(), value);
    }
  }

  static boolean isImmutable(Object value) {
    return value instanceof Immutable
        || value instanceof String
        || value instanceof Number;
  }

  @Override public Object put(Object o, Object o1) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public void putAll(Map<?, ?> map) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public void clear() {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }

  @Override public Object remove(Object o) {
    throw new LoopExecutionException(IMMUTABILITY_ERROR);
  }
}
