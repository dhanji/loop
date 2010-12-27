package loop.ast.script;

import loop.Parser;
import loop.ast.Node;
import loop.type.Type;

import java.util.List;

/**
 * A declaration of a function. May be free or a member of a class.
 */
public class FunctionDecl extends Node {
  private final String name;
  private final ArgDeclList arguments;

  private Type returnType;
  private List<Type> argumentTypes;

  // Memo field.
  private Type inferredType;

  /**
   * Flag that forces this function to be treated as non-polymorphic. Used
   * when the compiler is smart enough to tell the type of a function without
   * call-site quantification.
   */
  private boolean forceConcrete = false;

  public FunctionDecl(String name, ArgDeclList arguments) {
    this.name = name;
    this.arguments = arguments == null ? new ArgDeclList() : arguments;
  }

  public String name() {
    return name;
  }

  public Type getReturnType() {
    return inferredType;
  }

  public List<Type> getArgumentTypes() {
    return argumentTypes;
  }

  public ArgDeclList arguments() {
    return arguments;
  }

  public boolean isPolymorphic() {
    if (forceConcrete) {
      return false;
    }
    for (Node node : arguments.children()) {
      ArgDeclList.Argument argument = (ArgDeclList.Argument) node;

      // If any of the arguments are polymorphic, then the entire function
      // is polymorphic.
      if (argument.type() == null)
        return true;
    }
    return false;
  }

  @Override
  public String toSymbol() {
    return name + ": " + Parser.stringify(arguments) + " ->";
  }
}
