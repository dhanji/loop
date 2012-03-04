package loop.lang;

/**
 * Represents a type in loop.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopClass {
  public final String name;

  public LoopClass(String name) { this.name = name; }

  @Override public String toString() {
    return name;
  }
}
