package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StringLiteral extends Node {
  public final String value;

  public StringLiteral(String value) {
    // Strip quotes...
    this.value = value;
  }

  @Override
  public String toSymbol() {
    return value;
  }

  @Override
  public String toString() {
    return "String{" +
        "'" + value + '\'' +
        '}';
  }
}
