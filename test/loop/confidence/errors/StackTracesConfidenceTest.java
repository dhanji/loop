package loop.confidence.errors;

import loop.Loop;
import org.junit.Test;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StackTracesConfidenceTest {
  @Test
  public final void stackTracing1() {
    Loop.run("test/loop/confidence/errors/stack_traces_1.loop");
  }
}
