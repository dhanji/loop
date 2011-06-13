package loop.ast;

/**
 * A call chain of dereferences or method calls, strung together.
 */
public class CallChain extends Node {
  @Override
  public String toSymbol() {
    return ".";
  }
}
