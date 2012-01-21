package loop.confidence;

import loop.Loop;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ControlFlowConfidenceTest {
  @Test
  public final void ifThenElse() {
    assertEquals("evenodd", Loop.run("test/loop/confidence/cflow/if-then-else.loop"));
  }

  @Test
  public final void ifThenElseInExpression() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 2);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/if-then-else_2.loop", true));
  }

  @Test
  public final void ifThenElseInExpressionAlt() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 1);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/if-then-else_3.loop", true));
  }
}
