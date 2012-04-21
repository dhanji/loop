package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class DoubleLiteral extends Node {
  public final double value;
  public DoubleLiteral(String value) {
    this.value = Double.parseDouble(value);
  }

  @Override
  public String toString() {
    return "DoubleLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
