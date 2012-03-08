package loop.runtime;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Closure {
  public final String name;
  public final Object[] freeVariables;

  public Closure(String name) {
    this.name = name;
    this.freeVariables = Caller.EMPTY_ARRAY;
  }

  public Closure(String name, Object[] freeVariables) {
    this.name = name;
    this.freeVariables = freeVariables;
  }
}
