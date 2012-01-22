package loop.confidence.interop;

import loop.Loop;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class JavaInteropConfidenceTest {
  @Test
  public final void createAndCallAnonymousFunction() {
    assertEquals("hello", Loop.run("test/loop/confidence/interop/postfix_call_1.loop"));
  }
}
