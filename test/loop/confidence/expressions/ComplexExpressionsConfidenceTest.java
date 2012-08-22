package loop.confidence.expressions;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Regression test for expression edge cases.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ComplexExpressionsConfidenceTest extends LoopTest {
  @Test
  public final void slicingArrays() {
    assertArrayEquals(new Object[]{ "there", "dude", "1" },
        (Object[]) Loop.run("test/loop/confidence/expressions/array_slice_1.loop"));
  }

  @Test
  public final void slicingArraysFrom() {
    assertArrayEquals(new Object[]{ "dude", "1" },
        (Object[]) Loop.run("test/loop/confidence/expressions/array_slice_2.loop"));
  }

  @Test
  public final void slicingArraysTo() {
    assertArrayEquals(new Object[]{ "hi", "there", "dude" },
        (Object[]) Loop.run("test/loop/confidence/expressions/array_slice_3.loop"));
  }

  @Test
  public final void mutatingArraysInPlace() {
    assertArrayEquals(new Object[]{ "hi", "there", "dude", "!!" },
        (Object[]) Loop.run("test/loop/confidence/expressions/array_mutation.loop"));
  }

  @Test
  public final void assignAndReturn() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/expressions/assign_ret_1.loop"));
  }
}
