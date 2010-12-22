package loop.ast.script;

import loop.Reducer;

import java.util.*;

/**
 * A compilation unit containing imports classes, functions, etc. Represents a single file.
 */
public class Unit {
  private final ModuleDecl module;

  private final Set<RequireDecl> imports = new HashSet<RequireDecl>();
  private final Map<String, FunctionDecl> functions = new LinkedHashMap<String, FunctionDecl>();
//  private final Map<String, FunctionDecl> classes = new HashMap<String, FunctionDecl>();

  public Unit(ModuleDecl module) {
    this.module = module;
  }

  public void reduceAll() {
    for (FunctionDecl functionDecl : functions.values()) {
      new Reducer(functionDecl).reduce();
    }
  }

  public FunctionDecl get(String name) {
    return functions.get(name);
  }

  public void add(FunctionDecl node) {
    functions.put(node.name(), node);
  }

  public void add(RequireDecl node) {
    imports.add(node);
  }

  public Collection<FunctionDecl> functions() {
    return functions.values();
  }
}
