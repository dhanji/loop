package loop.type.scope;

import loop.ast.script.FunctionDecl;
import loop.type.Type;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class FunctionBinding {
  public final List<Type> argTypes;
  public final Type type;
  public final FunctionDecl function;

  public FunctionBinding(Symbol symbol, List<Type> argTypes, Type type) {
    this.function = symbol.function;
    this.argTypes = argTypes;
    this.type = type;
  }
}
