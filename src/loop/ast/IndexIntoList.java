package loop.ast;

import loop.Parser;

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
  public String toSymbol() {
    return "["
        + (from == null ? "" : Parser.stringify(from))
        + (slice ? ".." : "")
        + (to == null ? "" : Parser.stringify(to))
        + "]";
  }
}
