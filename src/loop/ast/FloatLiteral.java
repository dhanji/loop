package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class FloatLiteral extends Node {
  public final float value;
  public FloatLiteral(String value) {
    // Strip trailing 'L'
    this.value = Float.parseFloat(value.substring(0, value.length() - 1));
  }

  @Override
  public String toString() {
    return "FloatLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
