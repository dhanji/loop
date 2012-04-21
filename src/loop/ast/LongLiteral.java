package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LongLiteral extends Node {
  public final long value;
  public LongLiteral(String value) {
    // Strip trailing 'L'
    this.value = Long.parseLong(value.substring(0, value.length() - 1));
  }

  @Override
  public String toString() {
    return "LongLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
