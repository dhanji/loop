package loop;

import loop.ast.Variable;
import loop.ast.script.FunctionDecl;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Context {
  private static final AtomicInteger localVariableNameSequence = new AtomicInteger();

  String name;
  final List<String> arguments = new ArrayList<String>();
  final Map<String, Integer> argumentIndex = new HashMap<String, Integer>();
  final List<String> localVars = new ArrayList<String>();
  final Map<String, Integer> localVarIndex = new HashMap<String, Integer>();

  final Map<String, FunctionDecl> localFunctions = new HashMap<String, FunctionDecl>();

  final Label endOfFunction = new Label();

  public Context(String name) {
    this.name = name;
  }

  public void newLocalFunction(String localName, FunctionDecl func) {
    localFunctions.put(localName, func);
  }

  public FunctionDecl localFunctionName(String localName) {
    return localFunctions.get(localName);
  }

  public String newLocalVariable() {
    String var = "$__" + localVariableNameSequence.incrementAndGet();
    localVars.add(var);
    localVarIndex.put(var, localVars.size());

    return var;
  }

  public String newLocalVariable(Variable var) {
    if (arguments.isEmpty()) {
      localVarIndex.put(var.name, localVars.size());
      localVars.add(var.name);
    } else {
      localVars.add(var.name);
      localVarIndex.put(var.name, localVars.size());
    }
    return var.name;
  }

  public int newLocalVariable(String localVar) {
    localVars.add(localVar);
    localVarIndex.put(localVar, localVars.size());
    return localVars.size();
  }

  public void newFreeVariable(Variable freeVariable) {
    localVarIndex.put(freeVariable.name, arguments.size());
  }

  public int localVarIndex(String name) {
    return localVarIndex.get(name);
  }
}
