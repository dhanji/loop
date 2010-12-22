package loop.ast;

import loop.compile.LocalVar;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class Variable extends Node {
  public final String name;
  private Type type = Types.INTEGER; // defaults to int
  public String value = Types.INTEGER.defaultValue();

  public Variable(String name) {
    this.name = name;
  }

  public void setEgressType(Scope scope, Type type, boolean isArgument) {
//    scope.load(this, isArgument);

    this.type = type;
    this.value = type.defaultValue();
  }

  @Override
  public Type egressType(Scope scope) {
    LocalVar variable = scope.getLocalVariable(name);

    // If this variable is already declared in this scope,
    // then use its type.
    if (variable != null) {
      this.type = variable.getType();
    } else {
      scope.errors().unknownSymbol(name);
    }
    return type;
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
