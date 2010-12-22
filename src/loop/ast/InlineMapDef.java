package loop.ast;

import loop.LoopCompiler;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * Inline map definition. Entries in the map are declared
 * by alternating keys/values as 1st-level children of this node.
 */
public class InlineMapDef extends Node {
  private final boolean isTree;

  public InlineMapDef(boolean isTree) {
    this.isTree = isTree;
  }

  @Override
  public Type egressType(Scope scope) {
    return Types.MAP;
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    loopCompiler.write(isTree ? "Trees" : "Maps");
    loopCompiler.write(".of(");

    // The map parameter type is determined from the most general key type
    // and the most general value type. For now though we'll just use the
    // type of the 'first' entry.
    Type keyType, valueType;
    if (children.isEmpty()) {
      keyType = valueType = Types.VOID;
    } else {
      // Keys/values alternate
      keyType = children.get(0).egressType(loopCompiler.currentScope());
      valueType = children.get(1).egressType(loopCompiler.currentScope());
    }

    loopCompiler.write("new Object[]{");

    for (int i = 0; i < children.size(); i += 2) {
      Node key = children.get(i);

      // Type check key.
      Type typeOfKey = key.egressType(loopCompiler.currentScope());
      loopCompiler.errors().check(keyType, typeOfKey, "map key");

      // Account for primitives that need to be boxed.
      boolean shouldBox = Types.isPrimitive(keyType);
      if (shouldBox) {
        loopCompiler.write("new ");
        loopCompiler.write(Types.boxedTypeOf(keyType));
        loopCompiler.write("(");
      }

      key.emit(loopCompiler);

      if (shouldBox) {
        loopCompiler.write(")");
      }

      loopCompiler.write(", ");
      
      Node value = children.get(i + 1);
      // Type check value.
      Type typeOfValue = value.egressType(loopCompiler.currentScope());
      loopCompiler.errors().check(valueType, typeOfValue, "map value");

      // Account for primitives that need to be boxed.      
      shouldBox = Types.isPrimitive(valueType);
      if (shouldBox) {
        loopCompiler.write("new ");
        loopCompiler.write(Types.boxedTypeOf(valueType));
        loopCompiler.write("(");
      }

      value.emit(loopCompiler);

      if (shouldBox) {
        loopCompiler.write(")");
      }

      if (i < children.size() - 2)
        loopCompiler.write(", ");
    }

    loopCompiler.write("})");
  }

  @Override
  public String toSymbol() {
    return isTree ? "tree" : "map";
  }
}
