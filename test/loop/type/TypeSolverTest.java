package loop.type;

import loop.CompilingInterpreter;
import loop.Parser;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.type.scope.BaseScope;
import loop.type.scope.FunctionBinding;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class TypeSolverTest {

  private Unit unit;
  private BaseScope scope;

  @Before
  public void setUp() throws Exception {
    scope = new BaseScope();
  }

  @Test
  public final void simpleExpressionEgressTypeResolve() {
    assertEquals(Types.INTEGER, solve("simple_expr.loop"));
  }

  @Test(expected = RuntimeException.class)
  public final void simpleExpressionEgressTypeUnbound() {
    assertEquals(Types.INTEGER, solve("simple_expr_unbound.loop"));
    fail();
  }

  @Test
  public final void simpleExpressionEgressTypeBound() {
    assertEquals(Types.INTEGER, solve("simple_expr_bound.loop"));
  }

  @Test
  public final void multilineExpressionEgressTypeBound() {
    assertEquals(Types.INTEGER, solve("multi_expr_bound.loop"));
  }

  @Test
  public final void simpleExpressionEgressTypeBound2() {
    assertEquals(Types.INTEGER, solve("simple_expr_bound2.loop"));
  }

  @Test(expected = RuntimeException.class)
  public final void simpleExpressionEgressTypeConflict() {
    solve("simple_expr_conflict.loop");
    fail();
  }

  @Test
  public final void simpleExpressionCallsiteBinding() {
    assertNull(solve("simple_callsite_bound.loop", "main"));

    // Should have bound func with (int, int)
    assertEquals(1, scope.functionBindings().size());
    FunctionBinding binding = scope.functionBindings().get(0);

    assertNotNull(binding);
    assertEquals(2, binding.argTypes.size());
    assertEquals(Arrays.asList(Types.INTEGER, Types.INTEGER), binding.argTypes);

    // Now we should be able to solve for the type of 'func' given the callsite binding.
    assertEquals(Types.INTEGER, TypeSolver.solve(unit.get("func"), scope, binding.argTypes));
  }

  @Test
  public final void polymorphicCallsiteBinding() {
    assertNull(solve("polymorph_callsite_bound.loop", "main"));

    // Should have bound func with (int, int)
    assertEquals(2, scope.functionBindings().size());
    FunctionBinding binding = scope.functionBindings().get(0);

    assertNotNull(binding);
    assertEquals(2, binding.argTypes.size());
    assertEquals(Arrays.asList(Types.INTEGER, Types.INTEGER), binding.argTypes);

    // Now we should be able to solve for the type of 'add' given the callsite binding.
    assertEquals(Types.INTEGER, TypeSolver.solve(unit.get("add"), scope, binding.argTypes));


    // Now re-solve this with a (string, string) callsite.
    binding = scope.functionBindings().get(1);
    assertNotNull(binding);
    assertEquals(2, binding.argTypes.size());
    assertEquals(Arrays.asList(Types.STRING, Types.STRING), binding.argTypes);

    // Now we should be able to solve for the type of 'func' given the callsite binding.
    assertEquals(Types.STRING, TypeSolver.solve(unit.get("add"), scope, binding.argTypes));
  }

  private Type solve(String script, String funcName) {
    unit = CompilingInterpreter.load(
        new InputStreamReader(TypeSolverTest.class.getResourceAsStream(script)));

    // Register all functions as symbols in the base scope of this compilation unit.
    for (FunctionDecl functionDecl : unit.functions()) {
      scope.load(functionDecl);
    }

    // Read lone function.
    FunctionDecl func = unit.get(funcName);
    Type type = TypeSolver.solve(func, scope);
    System.out.println(Parser.stringify(func) + " :: " + type);

    return type;
  }

  private Type solve(String name) {
    return solve(name, "func");
  }
}
