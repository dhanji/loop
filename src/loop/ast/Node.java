package loop.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract node in the parse tree.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public abstract class Node {
  // the rest of the tree under this node
  protected final List<Node> children = new ArrayList<Node>();

  public int sourceLine, sourceColumn;

  public Node add(Node child) {
    children.add(child);
    return this;
  }

  public List<Node> children() {
    return children;
  }

  public Node onlyChild() {
    assert children.size() == 1;
    return children.get(0);
  }

  public abstract String toSymbol();

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        children + '}';
  }
}
