package loop.compile;

import loop.ast.Variable;
import loop.ast.script.FunctionDecl;
import loop.type.Errors;
import loop.type.Type;

import java.util.Collection;
import java.util.List;

/**
 * A lexical scope in jade source code.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Scope {
  void declareArgument(String name, Type type);

  void load(FunctionDecl func);

  void load(Type type);

  FunctionDecl getFunction(String name);

  Type getType(String name);

  Scope parent();

  Errors errors();

  void witness(FunctionDecl functionDecl, List<Type> bound, Type inferred);

  List<BasicScope.Witness> getWitnesses();

  LocalVar getLocalVariable(String name);

  /**
   * Binds the given argument variable to a concrete type.
   * @param name The name of an argument to the current function
   * @param type The concrete type to bind the given argument to
   */
  void witnessArgument(String name, Type type);

  Type getInferredArgumentType(String name);

  Collection<LocalVar> getVariables();

  void maybeDeclare(Variable var);

  class Witness {
    public final FunctionDecl functionDecl;
    public final Type returnType;
    public final List<Type> argumentTypes;

    public Witness(FunctionDecl functionDecl, List<Type> argumentTypes, Type returnType) {
      this.functionDecl = functionDecl;
      this.argumentTypes = argumentTypes;
      this.returnType = returnType;
    }
  }
}
