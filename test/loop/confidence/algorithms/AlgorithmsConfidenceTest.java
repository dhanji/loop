package loop.confidence.algorithms;

import loop.Loop;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class AlgorithmsConfidenceTest {
  @Test
  public final void insertsort() {
    assertEquals(Arrays.asList(1, 2, 3), Loop.run("test/loop/confidence/algorithms/insertsort.loop"));
  }

  @Test
  public final void quicksort() {
    assertEquals(Arrays.asList(0, 1, 2, 5, 6, 19, 92, 144),
        Loop.run("test/loop/confidence/algorithms/quicksort.loop"));
  }

//  @Test
  public final void mergesort() {
    assertEquals(Arrays.asList(0, 1, 2, 5, 6, 19, 92, 144),
        Loop.run("test/loop/confidence/algorithms/mergesort.loop", true));
  }

  @Test // DISABLED WHILE MVEL FIX GOES IN TO 2.1.Final
  public final void ransomNote() {
    assertEquals(new HashMap<Character, Integer>(),
        Loop.run("test/loop/confidence/algorithms/ransom_note.loop", true));
  }

  @Test
  public final void djikstra() {
    assertEquals(new HashMap<Character, Integer>(),
        Loop.run("test/loop/confidence/algorithms/djikstra.loop", true));
  }
}
