package loop;

import loop.ast.Variable;
import loop.compile.Scope;
import loop.type.Errors;

/**
 * Low level binary emitting api.
 */
public interface LoopCompiler {
  void write(String st);
  void write(int value);
  void writeAtMarker(String st);

  void mark();

  Scope currentScope();

  Errors errors();
}
