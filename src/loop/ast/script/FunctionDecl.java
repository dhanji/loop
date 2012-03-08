package loop.ast.script;

import loop.Parser;
import loop.ast.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * A declaration of a function. May be free or a member of a class.
 */
public class FunctionDecl extends Node {
  private String name;
  private final ArgDeclList arguments;
  public boolean patternMatching;
  public final boolean isPrivate;

  public final List<Node> whereBlock = new ArrayList<Node>();

  public FunctionDecl(String name, ArgDeclList arguments) {
    this.name = name;
    this.isPrivate = name == null || name.startsWith("@");
    this.arguments = arguments == null ? new ArgDeclList() : arguments;
  }

  public String name() {
    return name;
  }

  public ArgDeclList arguments() {
    return arguments;
  }

  public void name(String name) {
    this.name = name;
  }

  @Override
  public String toSymbol() {
    return (name == null ? "<anonymous>" : name) + ": " + Parser.stringify(arguments) + " ->";
  }
}
