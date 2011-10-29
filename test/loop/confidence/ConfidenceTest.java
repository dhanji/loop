package loop.confidence;

import loop.Loop;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests runs a bunch of realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ConfidenceTest {
  @Test
  public final void reverseListPatternMatching() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/reverse.loop"));
  }

  @Test
  public final void reverseStringPatternMatching() {
    assertEquals("olleh", Loop.run("test/loop/confidence/reverse_string.loop"));
  }

  @Test
  public final void splitLinesStringPatternMatching() {
    assertEquals("hellotheredude", Loop.run("test/loop/confidence/split_lines_string.loop"));
  }

  @Test
  public final void splitVariousStringsPatternMatching() {
    assertEquals("1234", Loop.run("test/loop/confidence/split_various_string.loop"));
  }

  @Test
  public final void splitVariousStringsPatternMatchingWithWildcards() {
    assertEquals("3", Loop.run("test/loop/confidence/split_various_selective.loop"));
  }

  @Test(expected = RuntimeException.class)
  public final void splitVariousStringsPatternMatchingNotAllMatches() {
    assertEquals("1234", Loop.run("test/loop/confidence/split_various_string_error.loop"));
  }

  @Test(expected = RuntimeException.class)
  public final void reverseLoopPatternMissingError() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/reverse_error.loop"));
  }
}
