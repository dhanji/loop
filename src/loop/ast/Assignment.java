package loop.ast;

import loop.LoopCompiler;
import loop.Parser;
import loop.compile.Scope;
import loop.type.Type;

/**
 * A right to left assignment statement.
 */
public class Assignment extends Node {
  private final Node condition;
  public Assignment(Node condition) {
    this.condition = condition;
  }

  public Assignment() {
    this(null);
  }

  @Override
  public Type egressType(Scope scope) {
    // The right hand side determines the egress type of the left hand side
    // TODO may want to cache so we don't recompute the egress type each time?
    Node lhs = lhs();
    Type rhsEgressType = rhs().egressType(scope);
    if (lhs instanceof Variable) {
      // TODO if the scope contains this variable and it is of a different
      // type, explode.
      ((Variable)lhs).setEgressType(scope, rhsEgressType, false);
    }

    return rhsEgressType;
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    lhs().emit(loopCompiler);
    loopCompiler.write("=");
    rhs().emit(loopCompiler);
  }

  private Node lhs() {
    return children.get(0);
  }

  private Node rhs() {
    return children.get(1);
  }

  @Override
  public String toSymbol() {
    if (condition == null) {
      return "=";
    }
    return "=if" + Parser.stringify(condition);
  }
}
