package loop.runtime;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Closure {
  public static final String CALL_FORM = "@call";
  // The target class this is resolved into.
  public final String target;

  // The function name.
  public final String name;

  // Variables that we have closed over.
  public final Object[] freeVariables;

  public Closure(String target, String name) {
    this.target = target;
    this.name = name;
    this.freeVariables = Caller.EMPTY_ARRAY;
  }

  public Closure(String target, String name, Object[] freeVariables) {
    this.target = target;
    this.name = name;
    this.freeVariables = freeVariables;
  }

  @Override public String toString() {
    return "loop.runtime.Closure{" +
        "" + target + '#' +
        "" + name +
        "()}";
  }
}
