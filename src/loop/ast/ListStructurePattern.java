package loop.ast;

/**
 * Any list related pattern on the LHS of a pattern matching rule. But that
 * doesn't destructure the list. More of an exact match structure.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ListStructurePattern extends Node {

  @Override public String toSymbol() {
    return "[]";
  }
}
