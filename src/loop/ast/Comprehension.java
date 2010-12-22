package loop.ast;

import loop.Parser;

/**
 * A list comprehension
 */
public class Comprehension extends Node {
  private final Node var;
  private Node inList;
  private Node filter;

  public Comprehension(Node var, Node inList, Node filter) {
    this.var = var;
    this.inList = inList;
    this.filter = filter;
  }

  public Node filter() {
    return filter;
  }

  public void filter(Node filter) {
    this.filter = filter;
  }

  public Node inList() {
    return inList;
  }

  public void inList(Node inList) {
    this.inList = inList;
  }

  @Override
  public String toSymbol() {
    return "for " + var.toSymbol() + " in " + Parser.stringify(inList)
        + (filter == null ? "" : " if " + Parser.stringify(filter));
  }
}
