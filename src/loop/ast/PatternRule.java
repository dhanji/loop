package loop.ast;

import loop.Parser;

/**
 * A single pattern matching rule, with a pattern LHS and and
 * potentially multiple guards/functions on the RHS.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class PatternRule extends Node {
  public Node pattern;
  public Node rhs;

  @Override public String toSymbol() {
    return "\n  => " + Parser.stringify(pattern)
        + ((rhs != null) ? " : " + Parser.stringify(rhs) : "");
  }
}
