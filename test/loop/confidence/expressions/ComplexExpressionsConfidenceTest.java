package loop.confidence.expressions;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Regression test for expression edge cases.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ComplexExpressionsConfidenceTest extends LoopTest {
  @Test
  public final void assignAndReturn() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/expressions/assign_ret_1.loop"));
  }
}
