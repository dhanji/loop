package loop.ast;

/**
 * Same as the ?: operator in Java except with a flipped condition.
 */
public class TernaryUnlessExpression extends Node {
  @Override
  public String toSymbol() {
    return "unless-then-else";
  }
}
