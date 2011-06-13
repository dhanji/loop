package loop.ast;

/**
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class StringLiteral extends Node {
  private final String name;

  public StringLiteral(String name) {
    // Strip quotes...
    this.name = name;
  }

  @Override
  public String toSymbol() {
    return name;
  }

  @Override
  public String toString() {
    return "String{" +
        "'" + name + '\'' +
        '}';
  }
}