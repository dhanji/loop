package loop.ast;

import loop.LoopCompiler;
import loop.Parser;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * An array dereference. Can also be an array slice. For example:
 *
 * arr[x]
 *
 * or:
 *
 * arr[1..5]
 *
 * or:
 *
 * arr[a..b]
 *
 * It may also be a partial slice:
 *
 * arr[0..]
 *
 * or:
 *
 * arr[..5]
 *
 */
public class IndexIntoList extends Node {
  private Node from;
  private final boolean slice;
  private Node to;

  public IndexIntoList(Node from, boolean slice, Node to) {
    this.from = from;
    this.slice = slice;
    this.to = to;
  }

  public Node from() {
    return from;
  }

  public void from(Node from) {
    this.from = from;
  }

  public Node to() {
    return to;
  }

  public void to(Node to) {
    this.to = to;
  }

  public boolean isSlice() {
    return slice;
  }

  @Override
  public Type egressType(Scope scope) {
    // Bit hacky but it's the type of any of the items in the list.
    // or type list if this is a range selection
    if (slice) {
      return Types.LIST;
    }

    return from.egressType(scope);
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    if (null == from && null == to) {
      loopCompiler.errors().generic("Invalid list index range specified");
      return;
    }

    if (slice) {
      loopCompiler.write("subList(");
      if (null == from) {
        loopCompiler.write("0");
      } else {
        from.emit(loopCompiler);
      }
      loopCompiler.write(", ");
      if (null == to) {
      } else {
        to.emit(loopCompiler);
      }
      loopCompiler.write(")");
    } else {

      // For wrapper types, we need to use special methods.
      if (Types.isPrimitive(from.egressType(loopCompiler.currentScope()))) {
        loopCompiler.writeAtMarker("Lists.get(");
        loopCompiler.write(", ");
        from.emit(loopCompiler);
        loopCompiler.write(")");
      } else {
        loopCompiler.write("get(");
        from.emit(loopCompiler);
        loopCompiler.write(")");
      }
    }
  }

  @Override
  public String toSymbol() {
    return "["
        + (from == null ? "" : Parser.stringify(from))
        + (slice ? ".." : "")
        + (to == null ? "" : Parser.stringify(to))
        + "]";
  }
}
