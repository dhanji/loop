package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class BigIntegerLiteral extends Node {
  public final String value;
  public BigIntegerLiteral(String value) {
    this.value = value.replace("@", "");
  }

  @Override
  public String toString() {
    return "BigIntegerLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
