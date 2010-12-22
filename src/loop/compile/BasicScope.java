package loop.compile;

import loop.ast.Variable;
import loop.ast.script.FunctionDecl;
import loop.type.Errors;
import loop.type.Type;

import java.util.*;

/**
 * A symbol scope, of a lexical scoping model.
 */
public class BasicScope implements Scope {
  private final Map<String, LocalVar> variables = new LinkedHashMap<String, LocalVar>();

  private final Map<String, FunctionDecl> functions = new HashMap<String, FunctionDecl>();
  private final Map<String, Type> types = new HashMap<String, Type>();

  private final Map<String, String> variablesToArgumentNames = new HashMap<String, String>();
  private final Map<String, Type> argumentsToTypes = new HashMap<String, Type>();
  private final List<Witness> witnesses = new ArrayList<Witness>();
  private final Errors errors;

  private Scope parent;

  public BasicScope(Errors errors, Scope parent) {
    this.errors = errors;
    this.parent = parent;
  }

  public void load(FunctionDecl func) {
    functions.put(func.name(), func);
  }

  public void load(Type type) {
    types.put(type.name(), type);
  }

  public void declareArgument(String name, Type type) {
    // Arguments are named $1, $2, $3, etc. (Javassist argument bindings)
    variables.put(name, new LocalVar(name, type, "$" + variables.size()));
  }

  // Maybe declare.
  public void maybeDeclare(Variable var) {
    // TODO assert that the type is not being re-bound.
    variables.put(var.name, new LocalVar(var.name, var.getType()));
  }

  public Collection<LocalVar> getVariables() {
    return variables.values();
  }

  public FunctionDecl getFunction(String name) {
    FunctionDecl function = functions.get(name);

    // Keep looking up the scope chain.
    if (null == function && null != parent) {
      function = parent.getFunction(name);
    }

    return function;
  }

  public Type getType(String name) {
    Type type = types.get(name);

    // Keep looking up the scope chain.
    if (null == type && null != parent) {
      type = parent.getType(name);
    }
    return type;
  }

  public void witnessArgument(String name, Type type) {
    if (!variablesToArgumentNames.containsKey(name)) {
      // Ignore this for now...
//      errors.generic("Attempted witness of variable that doesn't exist in this scope, " + name);
    } else {
      argumentsToTypes.put(name, type);
    }
  }

  public Type getInferredArgumentType(String name) {
    return argumentsToTypes.get(name);
  }

  public LocalVar getLocalVariable(String name) {
    return variables.get(name);
  }

  public Scope parent() {
    return parent;
  }

  public Errors errors() {
    return errors;
  }

  /**
   * Witnesses the return type of a polymorphic function bound over a given set
   * of quantified argument types.
   *
   * @param functionDecl the polymorphic function declaration.
   * @param bound the argument list of concrete type arguments.
   * @param inferred the witness of {@code functionDecl}. I.e. it's
   *     concrete inferred type.
   */
  public void witness(FunctionDecl functionDecl, List<Type> bound, Type inferred) {
    // HACK(dhanji): Workaround for not knowing the owning scope of a function.
    // Witness it in the parent-most scope.
    if (parent == null)
      witnesses.add(new Witness(functionDecl, bound, inferred));
    else
      parent.witness(functionDecl, bound,inferred);
  }

  public List<Witness> getWitnesses() {
    return witnesses;
  }
}
