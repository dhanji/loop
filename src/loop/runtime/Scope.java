package loop.runtime;

import loop.ast.ClassDecl;
import loop.ast.script.FunctionDecl;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Scope {
  ClassDecl resolve(String fullyQualifiedName);

  void declare(ClassDecl clazz);

  void declare(FunctionDecl function);
}
