package loop.confidence.lists;

import loop.Loop;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ListComprehensionConfidenceTest {
  @Test
  public final void identityComprehension() {
    assertEquals(Arrays.asList(10, 20, 30), Loop.run("test/loop/confidence/lists/projection.loop"));
  }

  @Test
  public final void identityFilterComprehension() {
    assertEquals(Arrays.asList(10, 20), Loop.run("test/loop/confidence/lists/projection_filter.loop"));
  }

  @Test
  public final void expressionProjectComprehension() {
    assertEquals(Arrays.asList(100, 200, 300), Loop.run("test/loop/confidence/lists/projection_expr.loop"));
  }

}
