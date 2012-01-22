package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class JavaLiteral extends Node {
  public final String value;

  public JavaLiteral(String value) {
    this.value = value.substring(1, value.length() - 1);
  }

  @Override public String toSymbol() {
    return '`' + value + '`';
  }
}
