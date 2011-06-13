package loop.type.scope;

import loop.ast.Variable;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.type.Type;
import loop.type.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class BaseScope {
  private final Map<String, Symbol> symbols = new HashMap<String, Symbol>();

  // New function bindings, not the same as newly encountered symbols as a
  // function binding can only come into existence if a symbol already exists
  // for it.
  private final List<FunctionBinding> bindings = new ArrayList<FunctionBinding>();

  public BaseScope() {
    populateBaseTypes();
  }

  private void populateBaseTypes() {
    symbols.put(Types.INTEGER.name(), new Symbol(Types.INTEGER));
    symbols.put(Types.STRING.name(), new Symbol(Types.STRING));
  }

  public Symbol resolve(String name) {
    return symbols.get(name);
  }

  public void load(Variable variable, Type type) {
    symbols.put(variable.name, new Symbol(variable, type));
  }

  public void load(ArgDeclList.Argument argument, Type explicitType) {
    Type type = explicitType == null ? resolve(argument.type()).type : explicitType;

    symbols.put(argument.name(), new Symbol(argument, type));
  }

  public void load(FunctionDecl functionDecl) {
    symbols.put(functionDecl.name(), new Symbol(functionDecl));
  }

  /**
   * Returns a list of unbound symbols in the current scope (i.e. ones
   * with no type associated).
   */
  public List<Symbol> unbound() {
    List<Symbol> unbound = new ArrayList<Symbol>();
    for (Symbol symbol : symbols.values()) {
      if (symbol.type == null) {
        unbound.add(symbol);
      }
    }

    return unbound;
  }

  public List<FunctionBinding> functionBindings() {
    return bindings;
  }

  /**
   * Binds a concrete function to be emitted.
   */
  public void bindFunction(Symbol symbol, List<Type> argTypes, Type type) {
    bindings.add(new FunctionBinding(symbol, argTypes, type));
  }
}
