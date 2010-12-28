package loop.type;

import loop.ast.*;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.type.scope.BaseScope;
import loop.type.scope.LocalScope;
import loop.type.scope.Symbol;

import java.util.*;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class TypeSolver {
  private static final Map<Class<? extends Node>, Type> TYPE_MAP;

  static {
    TYPE_MAP = new HashMap<Class<? extends Node>, Type>();

    TYPE_MAP.put(IntLiteral.class, Types.INTEGER);
    TYPE_MAP.put(StringLiteral.class, Types.STRING);
  }

  public static Type solve(FunctionDecl fn, BaseScope parent) {
    return solve(fn, parent, null);
  }

  public static Type solve(FunctionDecl fn, BaseScope parent, List<Type> argTypes) {
    Type functionType = null;

    LocalScope scope = new LocalScope(parent);

    // Before function analysis, seed the scope with function arguments.
    seedArguments(fn, scope, argTypes);

    // Each child of the function decl is a separate line.
    for (Node node : fn.children()) {
      functionType = walkExpr("  ", node.children(), scope);

      // Encounter each statement as it may be a free standing symbol/expr.
      encounter(node, scope, functionType);
    }

    // Assert that no unbound free variables remain.
    List<Symbol> unbound = scope.unbound();
    if (!unbound.isEmpty()) {
      throw new RuntimeException("Unknown symbols exist in " + fn.name() + " " + unbound);
    }

    // For now, the type of the last statement is the return type.
    return functionType;
  }

  private static void seedArguments(FunctionDecl fn, LocalScope scope, List<Type> argTypes) {
    ArgDeclList arguments = fn.arguments();

    int i = 0;
    for (Node node : arguments.children()) {
      // Cast guaranteed by parser.
      ArgDeclList.Argument argument = (ArgDeclList.Argument) node;

      scope.load(argument, null != argTypes ? argTypes.get(i++) : null);
    }
  }

  // Walks a single-line expression, determining its egress type.
  private static Type walkExpr(String indent, List<Node> nodes, LocalScope scope) {
    Type type = null;

    // Recursive walk down the AST.
    for (Node node : nodes) {
      Type localType;

      // Recursive Case: Post order traversal.
      localType = walkExpr(indent + "  ", node.children(), scope);

      // Base Case: Solve by performing expression egress type-propagation.
      if (node.children().isEmpty() && localType == null) {
        localType = TYPE_MAP.get(node.getClass());
      }

      if (null != localType) {
        if (null == type)
          type = localType;

          // You cannot have an expression containing values of inconvertible types.
        else if (!type.equals(localType)) {
          throw new RuntimeException("Types are inconvertible in expression: "
              + type + " and " + localType);
        }
      }

      // If you encounter a symbol, resolve it according to the current scope.
      encounter(node, scope, type);
    }

    return type;
  }

  private static void encounter(Node node, LocalScope scope, Type type) {
    if (node instanceof Variable) {
      Variable var = (Variable) node;

      Symbol symbol = scope.resolve(var.name);

      // If the symbol already exists in this scope, attempt to verify that it is
      // of a compatible nature (i.e. same type).
      if (symbol != null) {
        Type existingType = symbol.type;

        // Type mismatch.
        if (type != null && existingType != null && !existingType.equals(type)) {
          throw new RuntimeException("Type mismatch: " + existingType + " expected, but was " + type);
        }
      } else {
        // Otherwise push this variable to the current scope.
        scope.load(var, type);
      }
    } else if (node instanceof Call) {
      Call call = (Call) node;

      // Determine concrete bindings for each argument type by walking them.
      List<Type> argTypes = new ArrayList<Type>(call.args().children().size());
      boolean shouldBind = true;
      for (Node argNode : call.args().children()) {
        Type argType = walkExpr("", Arrays.asList(argNode), scope);
        if (argType == null) {
          shouldBind = false;
          break;
        }
        argTypes.add(argType);
      }

      // Now bind this function if argument types were known
      if (shouldBind) {
        scope.bindFunction(call, argTypes, type);
      }
    }
  }
}
