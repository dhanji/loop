package loop.ast.script;

import loop.AnnotatedError;
import loop.Context;
import loop.Executable;
import loop.Reducer;
import loop.StaticError;
import loop.ast.ClassDecl;
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
 * A compilation unit containing imports classes, functions, etc. Represents a single file.
 *
 * @NotThreadSafe
 */
public class Unit implements Scope {
  private final ModuleDecl module;

  private String name;
  private final Set<RequireDecl> imports = new LinkedHashSet<RequireDecl>();

  // Resolved, compiled imports:
  private final Set<Executable> deps = new LinkedHashSet<Executable>();
  private final Map<String, Executable> aliasedDeps = new HashMap<String, Executable>();

  private final Map<String, FunctionDecl> functions = new LinkedHashMap<String, FunctionDecl>();
  private final Map<String, ClassDecl> classes = new HashMap<String, ClassDecl>();
  private final Stack<Context> scopes = new Stack<Context>();

  public Unit(ModuleDecl module) {
    this.module = module;

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

  @Override public String resolveJavaType(String name) {
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
  }

  public String name() {
    return name;
  }

  @Override public ClassDecl resolve(String fullyQualifiedName) {
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
      // Resolve in deps.
      for (Executable dep : deps) {
        functionDecl = dep.getScope().resolveFunction(name, false);

        if (functionDecl != null)
          return functionDecl;
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
    node.moduleName = name;
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

  public List<AnnotatedError> loadDeps(String file) {
    List<AnnotatedError> errors = null;
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

              errors.add(new StaticError("Imported file is missing a 'module' declaration" + ": "
                  + executable.file()
                  + "\n\nrequired in: " + file, 0, 0));
            } else {
              if (requireDecl.alias != null) {

                // remove aliased module after it is loaded.
                aliasedDeps.put(requireDecl.alias, executable);
                imports.remove(requireDecl);
              }
              deps.add(executable);
            }
          }
        }
      }
    }

    return errors;
  }
}
