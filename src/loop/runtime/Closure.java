package loop.runtime;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Closure {
  public final String name;
  public final Object[] args;

  public Closure(String name) {
    this.name = name;
    this.args = Caller.EMPTY_ARRAY;
  }

  public Closure(String name, Object[] args) {
    this.name = name;
    this.args = args;
  }
}
