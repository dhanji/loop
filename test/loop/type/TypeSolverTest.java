package loop.type;

import loop.CompilingInterpreter;
import loop.Parser;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import org.junit.Test;

import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class TypeSolverTest {

  @Test
  public final void simpleExpressionEgressTypeResolve() {
    assertEquals(Types.INTEGER, solve("simple_expr.loop"));
  }

  @Test(expected = RuntimeException.class)
  public final void simpleExpressionEgressTypeConflict() {
    solve("simple_expr_conflict.loop");
    fail();
  }

  private static Type solve(String name) {
    Unit unit = CompilingInterpreter.load(
        new InputStreamReader(TypeSolverTest.class.getResourceAsStream(name)));

    // Read lone function.
    FunctionDecl func = unit.functions().iterator().next();
    Type type = TypeSolver.solve(func);
    System.out.println(Parser.stringify(func) + " :: " + type);

    return type;
  }
}
