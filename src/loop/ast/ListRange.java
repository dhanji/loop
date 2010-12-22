package loop.ast;

import loop.Parser;

/**
 * An inline list. Can be a range or
 */
public class ListRange extends Node {
  private final Node from;
  private final boolean slice;
  private final Node to;

  public ListRange(Node from, boolean slice, Node to) {
    this.from = from;
    this.slice = slice;
    this.to = to;
  }

  @Override
  public String toSymbol() {
    return "list "
        + (from == null ? "" : Parser.stringify(from))
        + (slice ? ".." : "")
        + (to == null ? "" : Parser.stringify(to));
  }
}
