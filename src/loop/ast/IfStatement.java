package loop.ast;

import loop.Parser;

/**
 * A free standing if/then statement.
 */
public class IfStatement extends Node {
  private final Node ifPart;
  private final Node thenPart;
  private final Node elsePart;

  public IfStatement(Node ifPart, Node thenPart, Node elsePart) {
    this.ifPart = ifPart;
    this.thenPart = thenPart;
    this.elsePart = elsePart;
  }

  public IfStatement(Node ifPart, Node thenPart) {
    this(ifPart, thenPart, null);
  }

  @Override
  public String toSymbol() {
    return "if " + Parser.stringify(ifPart) + " then " + Parser.stringify(thenPart)
        + (null == elsePart ? "" : " else " + Parser.stringify(elsePart));
  }
}
