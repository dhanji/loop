package loop;

import loop.ast.Variable;
import org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class Context {
  private static final AtomicInteger localVariableNameSequence = new AtomicInteger();

  String name;
  final List<String> arguments = new ArrayList<String>();
  final Map<String, Integer> argumentIndex = new HashMap<String, Integer>();
  final List<String> localVars = new ArrayList<String>();
  final Map<String, Integer> localVarIndex = new HashMap<String, Integer>();

  final Label endOfFunction = new Label();

  public Context(String name) {
    this.name = name;
  }

  public String newLocalVariable() {
    String var = "$__" + localVariableNameSequence.incrementAndGet();
    localVars.add(var);
    localVarIndex.put(var, localVars.size());

    return var;
  }

  public String newLocalVariable(Variable var) {
    localVars.add(var.name);
    localVarIndex.put(var.name, localVars.size());
    return var.name;
  }

  public int newLocalVariable(String localVar) {
    localVars.add(localVar);
    localVarIndex.put(localVar, localVars.size());
    return localVars.size();
  }

  public int localVarIndex(String name) {
    return localVarIndex.get(name);
  }
}
