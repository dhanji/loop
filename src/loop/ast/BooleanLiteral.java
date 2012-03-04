package loop.ast;

import loop.Token;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class BooleanLiteral extends Node {
  public final boolean value;
  public BooleanLiteral(Token token) {
    this.value = token.kind == Token.Kind.TRUE;
  }

  @Override
  public String toString() {
    return "BooleanLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
