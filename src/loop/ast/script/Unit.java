package loop.ast.script;

import loop.Reducer;
import loop.ast.ClassDecl;
import loop.runtime.Scope;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A compilation unit containing imports classes, functions, etc. Represents a single file.
 */
public class Unit implements Scope {
  private final ModuleDecl module;

  private final Set<RequireDecl> imports = new HashSet<RequireDecl>();
  private final Map<String, FunctionDecl> functions = new LinkedHashMap<String, FunctionDecl>();
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();

  public Unit(ModuleDecl module) {
    this.module = module;
  }

  public void reduceAll() {
    for (FunctionDecl functionDecl : functions.values()) {
      new Reducer(functionDecl).reduce();
    }
  }

  @Override public ClassDecl resolve(String fullyQualifiedName) {
    return classes.get(fullyQualifiedName);
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

  public void declare(ClassDecl classDecl) {
    classes.put(classDecl.name, classDecl);
  }

  public void add(RequireDecl node) {
    imports.add(node);
  }

  public Collection<FunctionDecl> functions() {
    return functions.values();
  }
}
