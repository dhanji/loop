package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class RegexLiteral extends Node {
  public final String value;

  public RegexLiteral(String value) {
    this.value = value;
  }

  @Override
  public String toSymbol() {
    return '/' + value + '/';
  }

  @Override
  public String toString() {
    return "Regex{" +
        "'" + value + '\'' +
        '}';
  }
}
