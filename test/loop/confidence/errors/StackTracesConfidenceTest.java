package loop.confidence.errors;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are as
 * expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StackTracesConfidenceTest extends LoopTest {
  @Test
  public final void stackTracing1() {
    try {
      Loop.run("test/loop/confidence/errors/stack_traces_1.loop");
      fail();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public final void bestMatches() {
    try {
      Loop.run("test/loop/confidence/errors/missing_method_error.loop");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
