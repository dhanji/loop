package loop.runtime;

import loop.ast.ClassDecl;
import loop.ast.script.FunctionDecl;
import loop.ast.script.RequireDecl;

import java.util.Set;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Scope {
  ClassDecl resolve(String fullyQualifiedName);

  FunctionDecl resolveFunction(String fullyQualifiedName);

  void declare(RequireDecl require);

  void declare(ClassDecl clazz);

  void declare(FunctionDecl function);

  Set<RequireDecl> requires();
}
