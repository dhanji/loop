package loop.ast;

/**
 * Any list related pattern on the LHS of a pattern matching rule.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StringPattern extends Node {

  @Override public String toSymbol() {
    return "''";
  }
}
