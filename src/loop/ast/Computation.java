package loop.ast;

/**
 * Represents an expression fragment.
 */
public class Computation extends Node {
  @Override
  public String toSymbol() {
    return "comput";
  }
}
