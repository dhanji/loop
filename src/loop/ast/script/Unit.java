package loop.ast.script;

import loop.AnnotatedError;
import loop.Context;
import loop.Executable;
import loop.Reducer;
import loop.StaticError;
import loop.ast.ClassDecl;
import loop.ast.Node;
import loop.runtime.Scope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A compilation unit containing imports classes, functions, etc. Represents a single fileName.
 *
 * @NotThreadSafe
 */
public class Unit implements Scope {
  private static final Map<String, String> ALWAYS_IMPORTED = new HashMap<String, String>();

  static {
    ALWAYS_IMPORTED.put("Integer", "java.lang.Integer");
    ALWAYS_IMPORTED.put("Long", "java.lang.Long");
    ALWAYS_IMPORTED.put("Number", "java.lang.Number");
    ALWAYS_IMPORTED.put("Double", "java.lang.Double");
    ALWAYS_IMPORTED.put("String", "java.lang.String");
    ALWAYS_IMPORTED.put("Boolean", "java.lang.Boolean");
    ALWAYS_IMPORTED.put("BigInteger", "java.math.BigInteger");
    ALWAYS_IMPORTED.put("BigDecimal", "java.math.BigDecimal");
  }

  private final String fileName;

  private String name;
  private final Set<RequireDecl> imports = new LinkedHashSet<RequireDecl>();

  // Resolved, compiled imports:
  private final Set<Executable> deps = new LinkedHashSet<Executable>();
  private final Map<String, Executable> aliasedDeps = new HashMap<String, Executable>();

  private final Map<String, FunctionDecl> functions = new LinkedHashMap<String, FunctionDecl>();
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();
  private final Stack<Context> scopes = new Stack<Context>();
  private List<Node> initializer;

  public Unit(String fileName, ModuleDecl module) {
    this.fileName = fileName;

    StringBuilder builder = new StringBuilder();
    List<String> moduleChain = module.moduleChain;
    for (int i = 0, moduleChainSize = moduleChain.size(); i < moduleChainSize; i++) {
      String modulePart = moduleChain.get(i);
      builder.append(modulePart);

      if (i < moduleChainSize - 1)
        builder.append('_');
    }
    this.name = builder.toString();

    // Always require prelude, except for prelude itself =).
    if (!module.name.equals("prelude"))
      declare(new RequireDecl(Arrays.asList("prelude"), null));
  }

  @Override public String getModuleName() {
    return name;
  }

  @Override public void pushScope(Context context) {
    scopes.push(context);
  }

  @Override public void popScope() {
    scopes.pop();
  }

  public String getFileName() {
    return fileName;
  }

  @Override public String resolveJavaType(String name) {
    String resolved = ALWAYS_IMPORTED.get(name);
    if (resolved != null)
      return resolved;

    for (RequireDecl requireDecl : imports) {
      if (requireDecl.javaLiteral == null)
        continue;

      if (requireDecl.javaLiteral.endsWith(name))
        return requireDecl.javaLiteral;
    }
    return null;
  }

  public void reduceAll() {
    for (ClassDecl classDecl : classes.values()) {
      new Reducer(classDecl).reduce();
    }
    for (FunctionDecl functionDecl : functions.values()) {
      new Reducer(functionDecl).reduce();
    }

    if (initializer != null) {
      List<Node> reduced = new ArrayList<Node>(initializer.size());
      for (Node node : initializer) {
        reduced.add(new Reducer(node).reduce());
      }
      initializer = reduced;
    }
  }

  public String name() {
    return name;
  }

  @Override public ClassDecl resolve(String fullyQualifiedName, boolean scanDeps) {
    ClassDecl classDecl = classes.get(fullyQualifiedName);
    if (classDecl == null && scanDeps) {

      // Resolve one-level off, in deps, but no farther.
      for (Executable dep : deps) {
        classDecl = dep.getScope().resolve(fullyQualifiedName, false);

        if (classDecl != null)
          return classDecl;
      }
    }
    return classDecl;
  }

  @Override public ClassDecl resolveAliasedType(String alias, String type) {
    Executable dep = aliasedDeps.get(alias);
    if (dep == null)
      return null;

    return dep.getScope().resolve(type, false);
  }

  @Override
  public FunctionDecl resolveFunctionOnStack(String fullyQualifiedName) {
    // First resolve in local scope if possible.
    Context context = scopes.peek();
    if (context != null) {
      FunctionDecl func = context.localFunctionName(fullyQualifiedName);
      if (func != null)
        return func;

      // Look for recursion only if a local function did not hide us.
      if (fullyQualifiedName.equals(context.thisFunction.name()))
        return context.thisFunction;
    }
    return resolveFunction(fullyQualifiedName, true);
  }

  @Override public void declare(RequireDecl require) {
    if (require.alias != null && "prelude".equals(require.moduleChain.get(0))) {

      // Remove prelude if it is aliased.
      imports.remove(require);
      return;
    }

    imports.add(require);
  }

  @Override public FunctionDecl resolveFunction(String name, boolean scanDeps) {
    FunctionDecl functionDecl = functions.get(name);
    if (functionDecl == null && scanDeps) {

      // Resolve in deps. But skip their private functions (obviously).
      for (Executable dep : deps) {
        functionDecl = dep.getScope().resolveFunction(name, false);

        if (functionDecl != null) {
          if (functionDecl.isPrivate)
            functionDecl = null;
          else
            return functionDecl;
        }
      }
    }
    return functionDecl;
  }

  @Override public FunctionDecl resolveNamespacedFunction(String name, String namespace) {
    Executable executable = aliasedDeps.get(namespace);
    if (null == executable)
      return null;

    return executable.getScope().resolveFunction(name, false);
  }

  public ClassDecl getType(String name) {
    return classes.get(name);
  }

  public void declare(FunctionDecl node) {
    functions.put(node.name(), node);

    // Set this function's module name.
    node.setModule(name);
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

  public void addToInitializer(Node expression) {
    if (initializer == null)
      initializer = new ArrayList<Node>();
    initializer.add(expression);
  }

  public List<Node> initializer() {
    return initializer;
  }

  public List<AnnotatedError> loadDeps(String file) {
    List<AnnotatedError> errors = null;
    List<RequireDecl> toRemove = new ArrayList<RequireDecl>();
    for (RequireDecl requireDecl : imports) {
      if (requireDecl.moduleChain != null) {
        List<Executable> executables = ModuleLoader.loadAndCompile(requireDecl.moduleChain);
        if (executables == null) {
          if (errors == null)
            errors = new ArrayList<AnnotatedError>();

          errors.add(new StaticError("Unable to locate module: " + requireDecl.moduleChain,
              requireDecl.sourceLine, requireDecl.sourceColumn));
        } else {
          for (Executable executable : executables) {
            if (executable.hasErrors()) {
              if (errors == null)
                errors = new ArrayList<AnnotatedError>();

              errors.addAll(executable.getStaticErrors());
            } else if (ModuleDecl.DEFAULT.name.equals(executable.getScope().getModuleName())) {
              if (errors == null)
                errors = new ArrayList<AnnotatedError>();

              errors.add(new StaticError("Imported file " + executable.file()
                  + ".loop is missing a 'module' declaration\n\nrequired in: " + file,
                  requireDecl.sourceLine, requireDecl.sourceColumn));
            } else {
              if (requireDecl.alias != null) {

                // remove aliased module after it is loaded.
                aliasedDeps.put(requireDecl.alias, executable);
                toRemove.add(requireDecl);
              } else
                deps.add(executable);
            }
          }
        }
      }
    }

    imports.removeAll(toRemove);
    return errors;
  }
}
