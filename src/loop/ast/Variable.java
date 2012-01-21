package loop.ast;

import loop.type.Type;
import loop.type.Types;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Variable extends Node {
  public String name;
  private Type type = Types.INTEGER; // defaults to int
  public String value = Types.INTEGER.defaultValue();

  public Variable(String name) {
    this.name = name;
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toSymbol() {
    return name;
  }

  @Override
  public String toString() {
    return "Variable{" +
        "name='" + name + '\'' +
        '}';
  }
}
