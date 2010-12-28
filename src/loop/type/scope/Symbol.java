package loop.type.scope;

import loop.ast.Variable;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.type.Type;

/**
 * Represents a symbol used in a given scope or expression. Typically
 * a variable or function name.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Symbol {
  public final String name;
  public Type type;
  public FunctionDecl function;

  public final Kind kind;

  public Symbol(Variable variable, Type type) {
    this.name = variable.name;
    this.type = type;
    this.kind = Kind.VARIABLE;
  }

  public Symbol(ArgDeclList.Argument argument, Type type) {
    this.name = argument.name();
    this.type = type;

    kind = Kind.VARIABLE;
  }

  public Symbol(Type type) {
    this.name = type.name();
    this.type = type;

    kind = Kind.TYPE;
  }

  public Symbol(FunctionDecl functionDecl) {
    this.name = functionDecl.name();
    this.function = functionDecl;

    this.kind = Kind.FUNCTION;
  }

  public static enum Kind {
    VARIABLE, FUNCTION, TYPE,
  }

  @Override
  public String toString() {
    return kind + ":" + name;
  }
}
