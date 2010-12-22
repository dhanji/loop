package loop.ast.script;

import loop.Parser;
import loop.ast.CallArguments;
import loop.ast.Node;
import loop.ast.Variable;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

import java.util.ArrayList;
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

  /**
   * Simple type inference algorithm, uses egress type of expression block to
   * determine the function type, given type-bound arguments.
   */
  public Type inferType(Scope scope, CallArguments args) {
    if (inferredType != null)
      return inferredType;

    // Step 0: Make sure we are type compliant with the given arg list.
    List<Type> argumentTypes = arguments().getTypes(scope);
    if (argumentTypes != null) {
      for (int i = 0; i < argumentTypes.size(); i++) {
        scope.errors().check(argumentTypes.get(i), args.children().get(i).egressType(scope),
            "argument");
      }

      return null;
    }

    // Step 1: Bind all given arguments are in current scope as local vars.

    List<Type> bound = new ArrayList<Type>(args.children().size());
    for (int i = 0; i < arguments.children().size(); i++) {
      ArgDeclList.Argument argDecl = (ArgDeclList.Argument) arguments.children().get(i);

      Variable argument = new Variable(argDecl.name());
      // Determine the egress type.
      Type type = args.children().get(i).egressType(scope);

      // Set it as a local variable of the inferred type.
      argument.setEgressType(scope, type, true);

      // Add it to the function signature.
      bound.add(type);
    }

    return inferType(scope, bound);
  }

  /**
   * Same as {@link #inferType(loop.compile.Scope , loop.ast.CallArguments)} except that
   * it attempts to work out the argument types universally for this function. In other
   * words, it assumes that this is not a polymorphic function.
   */
  public Type inferType(Scope scope) {
    if (inferredType != null)
      return inferredType;
    // arguments().getTypes() should never be null if the function is concrete.
    return inferType(scope, arguments().getTypes(scope));
  }

  private Type inferType(Scope scope, List<Type> bound) {
    // Must go through all the child nodes and assign types to everything from
    // the argument list.

    // Step 2: Traverse each statement and determine its egress type.
    // This binds any further unbound symbols with inferred types.
    Type inferred = null;
    for (Node statement : children) {
      inferred = statement.egressType(scope);
    }

    // Step 3: Solve the return type of this function by taking the last statement's
    // egress type. Otherwise the function had an empty body.
    if (null == inferred)
      inferred = Types.VOID;

    // Step 4: Witness this solution so that the specific signature can be emitted
    // as a Java overload.
    if (!forceConcrete)
      scope.witness(this, bound, inferred);

    return inferredType = inferred;
  }

  public Type attemptInferType(Scope scope) {
    // Must go through all the child nodes and assign types to everything from
    // the argument list.

    // This is a blind inference attempt with no bound arg types.
    List<Type> bound = new ArrayList<Type>(arguments().children().size());

    // Step 2: Traverse each statement and determine its egress type.
    // This binds any further unbound symbols with inferred types.
    Type inferred = null;
    for (Node statement : children) {
      inferred = statement.egressType(scope);
    }

    // Step 3: Solve the return type of this function by taking the last statement's
    // egress type. Otherwise the function had an empty body.
    if (null == inferred)
      inferred = Types.VOID;

    // Step 3.5: If this is an attempt to infer a type on a function with no bound
    // argument types, try to see if all arguments are satisfied.
    for (Node node : arguments().children()) {
      ArgDeclList.Argument argument = (ArgDeclList.Argument)node;
      Type type = scope.getInferredArgumentType(argument.name());

      if (type == null) {
        // Type inference failed.
        return inferredType = inferred;
      }

      bound.add(type);
    }
    forceConcrete = true;
    arguments().setTypes(bound);
    argumentTypes = bound;

    return inferredType = inferred;
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
