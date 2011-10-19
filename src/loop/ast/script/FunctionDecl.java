package loop.ast.script;

import loop.Parser;
import loop.ast.Node;

/**
 * A declaration of a function. May be free or a member of a class.
 */
public class FunctionDecl extends Node {
  private final String name;
  private final ArgDeclList arguments;

  public FunctionDecl(String name, ArgDeclList arguments) {
    this.name = name;
    this.arguments = arguments == null ? new ArgDeclList() : arguments;
  }

  public String name() {
    return name;
  }

  public ArgDeclList arguments() {
    return arguments;
  }

  @Override
  public String toSymbol() {
    return (name == null ? "<anonymous>" : name) + ": " + Parser.stringify(arguments) + " ->";
  }
}
