package loop.ast.script;

import loop.Context;
import loop.Reducer;
import loop.ast.ClassDecl;
import loop.runtime.Scope;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A compilation unit containing imports classes, functions, etc. Represents a single file.
 */
public class Unit implements Scope {
  private final ModuleDecl module;

  private final Set<RequireDecl> imports = new LinkedHashSet<RequireDecl>();
  private final Map<String, FunctionDecl> functions = new LinkedHashMap<String, FunctionDecl>();
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();
  private final Stack<Context> scopes = new Stack<Context>();

  public Unit(ModuleDecl module) {
    this.module = module;
  }

  @Override public void pushScope(Context context) {
    scopes.push(context);
  }

  @Override public void popScope() {
    scopes.pop();
  }

  public void reduceAll() {
    for (ClassDecl classDecl : classes.values()) {
      new Reducer(classDecl).reduce();
    }
    for (FunctionDecl functionDecl : functions.values()) {
      new Reducer(functionDecl).reduce();
    }
  }

  public String name() {
    StringBuilder name = new StringBuilder();
    for (String part : module.moduleChain) {
      name.append(part).append("_");
    }
    return name.toString();
  }

  @Override public ClassDecl resolve(String fullyQualifiedName) {
    return classes.get(fullyQualifiedName);
  }

  @Override
  public FunctionDecl resolveFunction(String fullyQualifiedName) {
    // First resolve in local scope if possible.
    Context context = scopes.peek();
    if (context != null) {
      FunctionDecl func = context.localFunctionName(fullyQualifiedName);
      if (func != null)
        return func;
    }
    return functions.get(fullyQualifiedName);
  }

  @Override public void declare(RequireDecl require) {
    imports.add(require);
  }

  public FunctionDecl get(String name) {
    return functions.get(name);
  }

  public ClassDecl getType(String name) {
    return classes.get(name);
  }

  public void declare(FunctionDecl node) {
    functions.put(node.name(), node);
  }

  @Override public Set<RequireDecl> requires() {
    return imports;
  }

  public void declare(ClassDecl classDecl) {
    classes.put(classDecl.name, classDecl);
  }

  public Collection<FunctionDecl> functions() {
    return functions.values();
  }

  public Set<RequireDecl> imports() {
    return imports;
  }
}
