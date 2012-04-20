package loop.ast.script;

import loop.ast.Node;

/**
 * Represents a declaration of arguments of a function.
 */
public class ArgDeclList extends Node {
  public static class Argument extends Node {
    private final String name;
    private final String type;

    public Argument(String name, String type) {
      this.name = name;
      this.type = type;
    }

    public String name() {
      return name;
    }

    public String type() {
      return type;
    }

    @Override
    public String toSymbol() {
      return name + (type == null ? "" : ":" + type);
    }
  }

  @Override
  public String toSymbol() {
    return children().isEmpty() ? "()" : "()=";
  }
}
