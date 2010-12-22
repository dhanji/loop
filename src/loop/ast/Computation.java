package loop.ast;

import loop.LoopCompiler;
import loop.compile.Scope;
import loop.type.Type;

/**
 * Represents an expression fragment.
 */
public class Computation extends Node {

  @Override
  public Type egressType(Scope scope) {
    // All children should have a common type which will egress this
    // computation. Alternatively, they should egress a structural type.
    Type commonType = null;
    boolean anyArgs = false;
    for (Node child : children) {
      if (child instanceof Variable) {
        anyArgs = true;
      }
      if (commonType == null) {
        commonType = child.egressType(scope);
      } else {
        scope.errors().check(commonType, child.egressType(scope), "expression");
      }
    }

    // Reiterate, this time binding arguments to types.
    if (anyArgs) {
      for (Node child : children) {

        // If any of the components are arguments, attempt to resolve them
        // into a type.
        if (child instanceof Variable) {
          scope.witnessArgument(((Variable) child).name, commonType);
        }
      }
    }

    return commonType;
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    loopCompiler.write("(");
    for (Node child : children) {
      child.emit(loopCompiler);
    }
    loopCompiler.write(")");
  }

  @Override
  public String toSymbol() {
    return "comput";
  }
}
