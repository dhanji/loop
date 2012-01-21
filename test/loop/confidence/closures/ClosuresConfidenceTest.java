package loop.confidence.closures;

import loop.Loop;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClosuresConfidenceTest {
  @Test
  public final void createAndCallAnonymousFunction() {
    assertEquals(100, Loop.run("test/loop/confidence/closures/simple_closures_1.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunction2() {
    assertEquals(100, Loop.run("test/loop/confidence/closures/simple_closures_2.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunctionWithWheres() {
    assertEquals(4, Loop.run("test/loop/confidence/closures/simple_closures_3.loop"));
  }
}
