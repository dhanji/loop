package loop.type.scope;

import loop.ast.Call;
import loop.type.Type;

import java.util.List;

/**
 * A local functions scope. The narrowest possible of all scopes.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LocalScope extends BaseScope {
  private final BaseScope parent;

  public LocalScope(BaseScope parent) {
    this.parent = parent;
  }

  @Override
  public Symbol resolve(String name) {
    Symbol symbol = super.resolve(name);

    // Look in containing scope if the symbol was not present in this scope.
    if (null == symbol) {
      symbol = parent.resolve(name);
    }

    return symbol;
  }

  /**
   * Creates a new concrete function binding based on the given
   * argument types from a callsite. Example:
   * <pre>
   * add(1, 2) # binds add() as add(a: Integer, b: Integer)
   * </pre>
   */
  public void bindFunction(Call call, List<Type> argTypes, Type type) {
    Symbol symbol = resolve(call.name);
    if (symbol == null || symbol.kind != Symbol.Kind.FUNCTION) {
      throw new RuntimeException(call.name + " is not a function");
    }

    // Delegate to parent, which should continually delegate until an appropriate
    // scope is reached for this function to be bound.
    parent.bindFunction(symbol, argTypes, type);
  }
}
