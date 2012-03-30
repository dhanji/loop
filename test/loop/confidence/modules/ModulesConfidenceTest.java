package loop.confidence.modules;

import loop.Loop;
import loop.LoopCompileException;
import loop.LoopTest;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ModulesConfidenceTest extends LoopTest {
  @Test(expected = LoopCompileException.class)
  public final void requireFaultyLoopModule() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_loop_error_1.loop"));
  }

  @Test
  public final void requireJavaClass() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireJavaClassFails() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java_error.loop"));
  }
}
