package loop.ast;

import loop.LexprParser;

import java.util.ArrayList;
import java.util.List;

/**
 * A single pattern matching rule, with a pattern LHS and and
 * potentially multiple guards/functions on the RHS.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class PatternRule extends Node {
  public final List<Node> patterns = new ArrayList<Node>();
  public Node rhs;

  @Override public String toSymbol() {
    return "\n  => "
        + LexprParser.stringify(patterns)
        + ((rhs != null) ? " : " + LexprParser.stringify(rhs) : "");
  }
}
