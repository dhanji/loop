package loop.ast;

/**
 * A call chain of dereferences or method calls, strung together.
 */
public class CallChain extends Node {
  public boolean nullSafe = true;

  public void nullSafe(boolean nullSafe) {
    this.nullSafe = nullSafe;
  }

  @Override
  public String toSymbol() {
    return ".";
  }
}
