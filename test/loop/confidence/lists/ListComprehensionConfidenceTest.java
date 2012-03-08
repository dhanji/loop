package loop.confidence.lists;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ListComprehensionConfidenceTest extends LoopTest {
  @Test
  public final void identityComprehension() {
    assertEquals(Arrays.asList(10, 20, 30), Loop.run("test/loop/confidence/lists/projection.loop"));
  }

  @Test
  public final void identityFilterComprehension() {
    assertEquals(Arrays.asList(10, 20),
        Loop.run("test/loop/confidence/lists/projection_filter.loop"));
  }

  @Test
  public final void expressionProjectComprehension() {
    assertEquals(Arrays.asList(100, 200, 300),
        Loop.run("test/loop/confidence/lists/projection_expr.loop"));
  }

  @Test
  public final void expressionProjectComprehension2() {
    assertEquals(Arrays.asList(100, 200, 300, 400),
        Loop.run("test/loop/confidence/lists/projection_expr2.loop"));
  }

  @Test
  public final void expressionProjectComprehension3() {
    assertEquals(Arrays.asList(100, 200, 300, -1, 400),
        Loop.run("test/loop/confidence/lists/projection_expr3.loop"));
  }

  @Test
  public final void expressionProjectComprehension4() {
    assertEquals(Arrays.asList(100, 200, 300),
        Loop.run("test/loop/confidence/lists/projection_expr4.loop"));
  }

  @Test
  public final void expressionProjectComprehension5() {
    assertEquals(Arrays.asList(100, 200, 300, -1, 400),
        Loop.run("test/loop/confidence/lists/projection_expr5.loop"));
  }

  @Test
  public final void expressionProjectComprehension6() {
    assertEquals(Arrays.asList(100, 200, 300, 400, -1),
        Loop.run("test/loop/confidence/lists/projection_expr6.loop", true));
  }

  @Test
  public final void functionProjectComprehension() {
    assertEquals(Arrays.asList(20, 40, 60),
        Loop.run("test/loop/confidence/lists/projection_function.loop"));
  }

  @Test
  public final void functionProjectComprehensionAltFilter() {
    assertEquals(Arrays.asList(20, 40, 80),
        Loop.run("test/loop/confidence/lists/projection_function_2.loop"));
  }
}
