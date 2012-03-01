package loop;

import loop.ast.Node;

/**
 * A micro production emitter. These are composed to process and emit an entire a parse tree
 * in some target language/bytecode.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
interface Emitter {
  void emitCode(Node node);
}
