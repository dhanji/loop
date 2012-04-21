package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class BigDecimalLiteral extends Node {
  public final String value;
  public BigDecimalLiteral(String value) {
    this.value = value.replace("@", "");
  }

  @Override
  public String toString() {
    return "BigDecimalLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
