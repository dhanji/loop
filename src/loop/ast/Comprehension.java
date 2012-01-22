package loop.ast;

import loop.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * A list comprehension
 */
public class Comprehension extends Node {
  private List<Node> projection = new ArrayList<Node>();
  private final Variable var;
  private Node inList;
  private Node filter;

  public Comprehension(Node var, Node inList, Node filter) {
    this.var = (Variable) var;
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

  public void projection(List<Node> nodes) {
    projection.addAll(nodes);
  }

  public List<Node> projection() {
    return projection;
  }

  public Variable var() {
    return var;
  }

  @Override
  public String toSymbol() {
    StringBuilder symbol = new StringBuilder("(cpr ");
    for (Node child : projection) {
      symbol.append(Parser.stringify(child)).append(' ');
    }
    return symbol.append("for ")
        .append(var.toSymbol())
        .append(" in ")
        .append(Parser.stringify(inList))
        .append(filter == null ? "" : " if " + Parser.stringify(filter))
        .append(")")
        .toString();
  }
}
