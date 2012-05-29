package loop.confidence.closures;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClosuresConfidenceTest extends LoopTest {
  @Test
  public final void createAndCallAnonymousFunction() {
    assertEquals(100, Loop.run("test/loop/confidence/closures/simple_closures_1.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunction4() {
    assertEquals(100, Loop.run("test/loop/confidence/closures/simple_closures_4.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunction2() {
    assertEquals(100, Loop.run("test/loop/confidence/closures/simple_closures_2.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunctionWithWheres() {
    assertEquals(4, Loop.run("test/loop/confidence/closures/simple_closures_3.loop"));
  }

  @Test
  public final void createAndCallAnonymousFunctionWithWheres5() {
    assertEquals(4, Loop.run("test/loop/confidence/closures/simple_closures_5.loop"));
  }

  @Test
  public final void whereblock() {
    assertEquals(26208, Loop.run("test/loop/confidence/closures/whereblock.loop"));
  }

  @Test
  public final void explicitlyInvokeClosureMultipleArgs() {
    assertEquals(42, Loop.run("test/loop/confidence/closures/call_closures_1.loop"));
  }
}
