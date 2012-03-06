package loop.ast;

import loop.Parser;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class DestructuringPair extends Node {
  public final Node lhs;
  public Node rhs;

  public DestructuringPair(Node lhs, Node rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override public String toSymbol() {
    return Parser.stringify(lhs) + " <- " + Parser.stringify(rhs);
  }
}
