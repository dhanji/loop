package loop;

import loop.ast.ClassDecl;
import loop.ast.Node;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Parser {
  Node parse();

  Unit script(String file);

  List<AnnotatedError> getErrors();

  /*** In-function instruction parsing rules ***/
  Node line();

  FunctionDecl functionDecl();

  ClassDecl classDecl();
}
