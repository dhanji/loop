package loop.ast;

import loop.LexprParser;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Guard extends Node {
  public final Node expression;
  public final Node line;

  public Guard(Node expression, Node line) {
    this.expression = expression;
    this.line = line;
  }

  @Override public String toSymbol() {
    return "\n    | " + LexprParser.stringify(expression) + " : " + LexprParser.stringify(line);
  }
}
