package loop.type;

import loop.ast.IntLiteral;
import loop.ast.Node;
import loop.ast.StringLiteral;
import loop.ast.script.FunctionDecl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public static Type solve(FunctionDecl fn) {
    Type type = null;
    // Each child of the function decl is a separate line.
    for (Node node : fn.children()) {
      type = walk("  ", node.children());
    }

    // For now, the type of the last statement is the return type.
    return type;
  }

  // Walks a single-line expression, determining its egress type.
  private static Type walk(String indent, List<Node> nodes) {
    Type type = null;

    // Recursive walk down the AST.
    for (Node node : nodes) {
      Type localType;

      // Recursive Case: Post order traversal.
      localType = walk(indent + "  ", node.children());

      // Base Case: Solve by performing expression egress type-detection.
//      System.out.print(indent + node.getClass().getSimpleName() + " " + node.toSymbol());
      if (node.children().isEmpty() && localType == null) {
        localType = TYPE_MAP.get(node.getClass());
      }

      if (null != localType) {
          if (null == type)
            type = localType;
          else if (!type.equals(localType)) {
            throw new RuntimeException("Types are inconvertible in expression: "
                + type + " and " + localType);
          }
        }
    }

    return type;
  }
}
