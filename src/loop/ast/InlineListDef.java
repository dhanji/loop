package loop.ast;

/**
 * Inline list definition.
 */
public class InlineListDef extends Node {
  private final boolean isSet;

  public InlineListDef(boolean set) {
    isSet = set;
  }

  @Override
  public String toSymbol() {
    return isSet ? "set" : "list";
  }
}
