package loop.ast;

/**
 * Same as the ?: operator in Java.
 */
public class TernaryIfExpression extends Node {
  @Override
  public String toSymbol() {
    return "if-then-else";
  }
}
