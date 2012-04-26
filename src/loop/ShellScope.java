package loop;

import loop.ast.ClassDecl;
import loop.ast.script.FunctionDecl;
import loop.ast.script.RequireDecl;
import loop.runtime.Scope;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Simple scope that resolves constructors in the current context.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class ShellScope implements Scope {
  private final Set<RequireDecl> requires = new LinkedHashSet<RequireDecl>();
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();
  private final Map<String, FunctionDecl> functions = new HashMap<String, FunctionDecl>();
  private final Stack<Context> scopes = new Stack<Context>();

  @Override public String getModuleName() {
    return "_shell_";
  }

  @Override public FunctionDecl resolveFunction(String name, boolean scanDeps) {
    return resolveFunctionOnStack(name);
  }

  @Override public FunctionDecl resolveNamespacedFunction(String name, String namespace) {
    throw new RuntimeException();
  }

  @Override public void declare(RequireDecl require) {
    requires.add(require);
  }

  @Override public void declare(ClassDecl clazz) {
    classes.put(clazz.name, clazz);
  }

  @Override public void declare(FunctionDecl function) {
    functions.put(function.name(), function);
  }

  @Override public Set<RequireDecl> requires() {
    return requires;
  }

  @Override public void pushScope(Context context) {
    scopes.push(context);
  }

  @Override public void popScope() {
    scopes.pop();
  }

  @Override public String resolveJavaType(String name) {
    for (RequireDecl requireDecl : requires) {
      if (requireDecl.javaLiteral == null)
        continue;

      if (requireDecl.javaLiteral.endsWith(name))
        return requireDecl.javaLiteral;
    }
    return null;
  }

  @Override public ClassDecl resolve(String fullyQualifiedName, boolean b) {
    return classes.get(fullyQualifiedName);
  }

  @Override
  public FunctionDecl resolveFunctionOnStack(String fullyQualifiedName) {
    // First resolve in local scope if possible.
    Context context = scopes.peek();
    if (context != null) {
      FunctionDecl func = context.localFunctionName(fullyQualifiedName);
      if (func != null)
        return func;
    }
    return functions.get(fullyQualifiedName);
  }
}
