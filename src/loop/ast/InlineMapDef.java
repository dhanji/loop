package loop.ast;

/**
 * Inline map definition. Entries in the map are declared
 * by alternating keys/values as 1st-level children of this node.
 */
public class InlineMapDef extends Node {
  public final boolean isTree;

  public InlineMapDef(boolean isTree) {
    this.isTree = isTree;
  }

  @Override
  public String toSymbol() {
    return isTree ? "tree" : "map";
  }
}
