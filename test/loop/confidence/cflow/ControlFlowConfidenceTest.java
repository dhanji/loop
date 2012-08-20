package loop.confidence.cflow;

import loop.Loop;
import loop.LoopCompileException;
import loop.LoopExecutionException;
import loop.LoopTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ControlFlowConfidenceTest extends LoopTest {
  @Test
  public final void ifThenElse() {
    assertEquals("evenodd", Loop.run("test/loop/confidence/cflow/if-then-else.loop"));
  }

  @Test
  public final void ifThenElseInExpression() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 2);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/if-then-else_2.loop"));
  }

  @Test
  public final void unlessThenElseInExpression() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 2);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/unless-then-else.loop"));
  }

  @Test
  public final void ifThenElseInExpressionAlt() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 1);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/if-then-else_3.loop"));
  }

  @Test
  public final void ifThenElseInExpressionWithPatternMatching() {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    map.put(1, 1);
    map.put(2, 13);

    assertEquals(map, Loop.run("test/loop/confidence/cflow/if-then-else_pmatch.loop"));
  }

  @Test
  public final void ifThenElseInInGuard() {
    assertEquals(Arrays.asList(2), Loop.run("test/loop/confidence/cflow/if-then-else_pmatch_2.loop"));
  }

  @Test
  public final void exceptionHandlerDecl() {
    assertEquals(LoopExecutionException.class.getName(),
        Loop.run("test/loop/confidence/cflow/except_1.loop"));
  }

  @Test
  public final void exceptionHandlerDeclOrdering() {
    assertEquals(LoopExecutionException.class.getName(),
        Loop.run("test/loop/confidence/cflow/except_6.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void exceptionHandlerDeclWithErroneousExceptionClause() {
    assertEquals(true, Loop.run("test/loop/confidence/cflow/except_2.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void exceptionHandlerDeclWithNonExceptionClause() {
    assertEquals(true, Loop.run("test/loop/confidence/cflow/except_3.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void exceptionHandlerDeclWithImproperSignature() {
    assertEquals(true, Loop.run("test/loop/confidence/cflow/except_4.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void exceptionHandlerDeclNotPatternMatching() {
    assertEquals(true, Loop.run("test/loop/confidence/cflow/except_5.loop"));
  }
}
