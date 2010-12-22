package loop.compile;

import loop.ast.Node;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.type.Errors;
import loop.type.Type;
import loop.type.Types;

import java.util.List;

/**
 * Compiles a single function declaration with its body from Loop
 * to Java.
 */
class FunctionCompiler {
  private final Errors errors;
  private final Scope containingScope;

  private String currentIndentLevel = "";
  private StringBuilder out = new StringBuilder();

  public FunctionCompiler(Errors errors, Scope containingScope) {
    this.errors = errors;
    this.containingScope = containingScope;
  }

  CompiledFunction compileConcreteFunction(FunctionDecl func) {
    // Infer the return type of this function.
    // ...
    func.attemptInferType(containingScope);

    // Create a new lexical scope for every function.
    Scope scope = new BasicScope(errors, containingScope);

    // emit function signature.
    compileSignature(func, scope);

    // Bake signature of function, then move on to the body.
    String signature = out.toString();
    out = new StringBuilder("{\n");

    currentIndentLevel += "  ";
    int startOfFunction = out.length();

    // Compile each statement in the function body.
    compileBody(func, scope);

    // Emit any declarations at the top of the function body, but after the
    // signature.
    out.insert(startOfFunction, Variables.declare(scope.getVariables()));

    outdent();
    out.append(currentIndentLevel);
    out.append("}\n");

    // Load this newly minted function into the containing scope.
    containingScope.load(func);

    return new CompiledFunction(signature, out.toString());
  }

  private void compileSignature(FunctionDecl func, Scope scope) {

    // Emit function signature first.
    out.append(currentIndentLevel);
    out.append("public ");
    out.append(currentIndentLevel);
    out.append(func.getReturnType().javaType());
    out.append(" ");
    out.append(func.name());
    out.append("(");

    List<Type> argTypes = func.getArgumentTypes();
    List<Node> args = func.arguments().children();
    for (int i = 0; i < args.size(); i++) {
      ArgDeclList.Argument declaredArg = (ArgDeclList.Argument) args.get(i);
      out.append(argTypes.get(i).javaType());
      out.append(" ");
      out.append(declaredArg.name());

      // Load the argument as a variable into the current scope, binding
      // it to the argument type. This may be a Type variable rather than
      // a concrete type.
      scope.declareArgument(declaredArg.name(), argTypes.get(i));

      if (i < args.size() - 1)
        out.append(", ");
    }
    out.append(");\n");
  }

  private void compileBody(FunctionDecl func, Scope scope) {
    StatementCompiler compiler = new StatementCompiler(scope);

    // Compile each statement in turn and add it to the Java function body.
    for (int i = 0; i < func.children().size(); i++) {
      Node node = func.children().get(i);
      // Analyze and determine the type of each statement.
      Type egressType = node.egressType(scope);
      // ...

      out.append(currentIndentLevel);
      int startOfLine = out.length();

      out.append(compiler.compile(node));
      out.append(";\n");

      // If this is the last line, return whatever was on the stack.
      if (i == func.children().size() - 1) {

        // HACK(dhanji): Workaround for void return types in Java. Simply
        // return. In the future we will want to use a sentinel that is
        // coercible into a Java type.
        if (Types.VOID.equals(egressType)) {
          out.append(currentIndentLevel);
          out.append("\n  return;\n");
        } else
          out.insert(startOfLine, "return ");
      }
    }
  }

  private void outdent() {
    currentIndentLevel = currentIndentLevel.substring(0, currentIndentLevel.length() - 2);
  }
}
