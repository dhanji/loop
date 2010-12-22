package loop.ast;

import loop.Parser;
import loop.ast.script.FunctionDecl;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * Represents a method call or member dereference.
 */
public class Call extends Node {
  public final String name;
  public final boolean isFunction;

  private CallArguments args;
  private static final String PRINT = "System.out.println";

  public Call(String name, boolean function, CallArguments args) {
    // HACK! rewrites print calls to Java sout
    this.name = "print".equals(name) ? PRINT : name;
    this.isFunction = function;
    this.args = args;
  }

  public CallArguments args() {
    return args;
  }

  @Override
  public Type egressType(Scope scope) {
    // MAJOR HACK(dhanji): Special case print for now.
    if (PRINT.equals(name)) {
      // Because we short-circuit the print() case, we need to probe the ingress
      // types of the (single) argument. This triggers the witnessing of called
      // functions that are polymorphic in the argument expression.
      // In other words, we need to exercise the argument types in order to generate
      // overloads for any unbound functions.
      assert args.children().size() == 1;
      args.children().get(0).egressType(scope);

      return Types.VOID;
    }

    FunctionDecl function = scope.getFunction(name);
    if (null == function) {
      scope.errors().unknownFunction(name);
      return Types.VOID;
    }

    // Assert argument type mismatches if appropriate.
    return function.inferType(scope, args);
  }

  @Override
  public String toString() {
    return "Call{" + name + (isFunction ? args.toString() : "") + "}";
  }

  @Override
  public String toSymbol() {
    return name + (isFunction ? Parser.stringify(args) : "");
  }
}
