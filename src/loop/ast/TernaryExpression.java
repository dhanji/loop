package loop.ast;

/**
 * Same as the ?: operator in Java.
 */
public class TernaryExpression extends Node {
  @Override
  public String toSymbol() {
    return "if-then-else";
  }
}
