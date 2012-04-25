package loop.ast.script;

import loop.Parser;
import loop.ast.Node;
import loop.ast.PatternRule;
import loop.ast.TypeLiteral;
import loop.ast.Variable;
import loop.ast.WildcardPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * A declaration of a function. May be free or a member of a class.
 */
public class FunctionDecl extends Node {
  public static FunctionDecl STATIC_INITIALIZER = new FunctionDecl("<clinit>", null);

  public String moduleName;
  private String name;
  private final ArgDeclList arguments;
  public boolean patternMatching;
  public final boolean isPrivate;
  public String exceptionHandler;

  public final List<Node> whereBlock = new ArrayList<Node>();

  // Memo fields.
  public transient List<Variable> freeVariables;
  private String scopedName;

  public FunctionDecl(String name, ArgDeclList arguments) {
    this.name = name;
    this.isPrivate = name == null || name.startsWith("@");
    this.arguments = arguments == null ? new ArgDeclList() : arguments;
  }

  /**
   * If this is an exception handler, returns a list of exception types
   * that are handled. Corresponds 1:1 with pattern rules in the exception
   * handler declaration.
   */
  public List<String> handledExceptions() {
    List<String> exceptions = new ArrayList<String>(children.size());
    for (Node child : children) {
      assert child instanceof PatternRule;

      Node pattern = ((PatternRule) child).patterns.get(0);
      if (pattern instanceof TypeLiteral)
        exceptions.add(((TypeLiteral) pattern).name);
      else if (pattern instanceof WildcardPattern)
        exceptions.add("java.lang.Exception");
    }

    return exceptions;
  }

  public boolean isAnonymous() {
    return null == name;
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

  public String scopedName() {
    return scopedName == null ? name : scopedName;
  }

  public void scopedName(String newName) {
    this.scopedName = newName;
  }

  @Override
  public String toSymbol() {
    return (name == null ? "<anonymous>" : name) + ": " + Parser.stringify(arguments)
        + (exceptionHandler == null ? "" : " except " + exceptionHandler)
        + " ->";
  }
}
