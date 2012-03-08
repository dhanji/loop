package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class JavaLiteral extends Node {
  public final String value;
  public final String staticFieldAccess;

  public JavaLiteral(String value) {

    value = value.substring(1, value.length() - 1);
    String[] split = value.split("::");
    if (split.length > 1) {
      this.value = split[0];
      this.staticFieldAccess = split[1];
    } else {
      this.value = value;
      this.staticFieldAccess = null;
    }
  }

  @Override public String toSymbol() {
    return '`' + value + '`';
  }
}
