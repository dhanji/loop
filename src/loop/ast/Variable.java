package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Variable extends Node {
  public String name;
  public String type;

  public Variable(String name) {
    this.name = name;
  }

  @Override
  public String toSymbol() {
    return name + (type != null ? ("[" + type + ']') : "" );
  }

  @Override
  public String toString() {
    return "Variable{" +
        (type != null ? ("type='" + type + '\'') : "" ) +
        "name='" + name + '\'' +
        '}';
  }
}
