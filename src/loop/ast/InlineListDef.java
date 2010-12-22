package loop.ast;

import loop.LoopCompiler;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * Inline list definition.
 */
public class InlineListDef extends Node {
  private final boolean isSet;

  public InlineListDef(boolean set) {
    isSet = set;
  }

  @Override
  public Type egressType(Scope scope) {
    return Types.LIST;
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    // TODO when performing type inference.
    // The list type is the most general type derivable from all
    // elements in the list.

    // For now we'll just use the type of the first element.
    Type bagType;
    if(children.isEmpty()) {
      bagType = Types.VOID;
    } else {
      bagType = children.get(0).egressType(loopCompiler.currentScope());
    }

    loopCompiler.write(isSet ? "Sets" : "Lists");
    loopCompiler.write(".of(new ");
    loopCompiler.write(bagType.javaType());
    loopCompiler.write("[] {");

    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);

      // Type check children.
      loopCompiler.errors().check(bagType, child.egressType(loopCompiler.currentScope()),
          isSet ? "set element" : "list element");

      child.emit(loopCompiler);

      if (i < children.size() - 1)
        loopCompiler.write(", ");
    }
    loopCompiler.write("})");
  }

  @Override
  public String toSymbol() {
    return isSet ? "set" : "list";
  }
}
