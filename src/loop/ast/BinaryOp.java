package loop.ast;

import loop.Token;

/**
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class BinaryOp extends Node {
  public final Token operator;

  public BinaryOp(Token operator) {
    this.operator = operator;
  }

  @Override
  public String toString() {
    return "BinaryOp{" +
        "operator=" + operator +
        ":: " + children +
        '}';
  }

  @Override
  public String toSymbol() {
    return operator.value;
  }
}
