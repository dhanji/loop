package loop.ast;

import loop.Parser;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Guard extends Node {
  private final Node expression;
  private final Node line;

  public Guard(Node expression, Node line) {
    this.expression = expression;
    this.line = line;
  }

  @Override public String toSymbol() {
    return "\n    | " + Parser.stringify(expression) + " : " + Parser.stringify(line);
  }
}
