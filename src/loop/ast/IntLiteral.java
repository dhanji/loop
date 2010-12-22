package loop.ast;

import loop.LoopCompiler;
import loop.compile.Scope;
import loop.type.Type;
import loop.type.Types;

/**
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class IntLiteral extends Node {
  public final int value;
  public IntLiteral(String value) {
    this.value = Integer.parseInt(value);
  }

  @Override
  public Type egressType(Scope scope) {
    return Types.INTEGER;
  }

  @Override
  public void emit(LoopCompiler loopCompiler) {
    loopCompiler.write(value);
  }

  @Override
  public String toString() {
    return "IntLiteral{" +
        "value=" + value +
        '}';
  }

  @Override
  public String toSymbol() {
    return "" + value;
  }
}
