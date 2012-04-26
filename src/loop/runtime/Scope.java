package loop.runtime;

import loop.Context;
import loop.ast.ClassDecl;
import loop.ast.script.FunctionDecl;
import loop.ast.script.RequireDecl;

import java.util.Set;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Scope {
  ClassDecl resolve(String fullyQualifiedName, boolean scanDeps);

  FunctionDecl resolveFunctionOnStack(String fullyQualifiedName);

  void declare(RequireDecl require);

  void declare(ClassDecl clazz);

  void declare(FunctionDecl function);

  Set<RequireDecl> requires();

  void pushScope(Context context);

  void popScope();

  String resolveJavaType(String name);

  String getModuleName();

  FunctionDecl resolveFunction(String name, boolean scanDeps);

  FunctionDecl resolveNamespacedFunction(String name, String namespace);

  ClassDecl resolveAliasedType(String alias, String type);
}
