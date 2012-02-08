package loop;

import loop.ast.ClassDecl;
import loop.ast.script.FunctionDecl;
import loop.runtime.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple scope that resolves constructors in the current context.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class ShellScope implements Scope {
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();
  private final Map<String, FunctionDecl> functions = new HashMap<String, FunctionDecl>();

  @Override public void declare(ClassDecl clazz) {
    classes.put(clazz.name, clazz);
  }

  @Override public void declare(FunctionDecl function) {
    functions.put(function.name(), function);
  }

  @Override public ClassDecl resolve(String fullyQualifiedName) {
    return classes.get(fullyQualifiedName);
  }
}
