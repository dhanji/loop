package loop.confidence;

import loop.Loop;
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
public class BasicFunctionsConfidenceTest {
  @Test
  public final void reverseListPatternMatching() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/reverse.loop"));
  }

  @Test
  public final void reverseListPatternMatchingGuarded1() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/reverse_guarded_1.loop"));
  }

  @Test
  public final void reverseListPatternMatchingGuarded2() {
    // Doesn't reverse the list if the first element is >= 10.
    assertEquals(Arrays.asList(10, 20, 30), Loop.run("test/loop/confidence/reverse_guarded_2.loop"));
  }

  @Test
  public final void listPatternMatchingGuarded2() {
    // Doesn't reverse the list if the first element is >= 10.
    assertEquals(Arrays.asList(10, 2, 3), Loop.run("test/loop/confidence/list_pattern_guarded_1.loop"));
  }

  @Test
  public final void reverseListPatternMatchingUsingWhereBlock() {
    assertEquals(Arrays.asList(3, 2, 1), Loop.run("test/loop/confidence/whereblock_1.loop"));
  }

  @Test
  public final void reverseListPatternMatchingUsingNestedWhereBlocks() {
    assertEquals(Arrays.asList(4, 3, 2, 1), Loop.run("test/loop/confidence/whereblock_2.loop"));
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

  @Test
  public final void callJavaMethodOnString() {
    assertEquals("hello", Loop.run("test/loop/confidence/java_call_on_string.loop"));
  }

  @Test
  public final void intLiteralPatternMatching() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "Michael");
    map.put("age", "212");

    assertEquals(map, Loop.run("test/loop/confidence/literal_pattern_matching.loop", true));
  }

  @Test
  public final void wildcardPatternMatchingGuarded1() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("count", "10");

    assertEquals(map, Loop.run("test/loop/confidence/wildcard_pattern_matching_guarded_1.loop", true));
  }

  @Test
  public final void wildcardPatternMatchingGuarded2() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("count", "100");

    assertEquals(map, Loop.run("test/loop/confidence/wildcard_pattern_matching_guarded_2.loop", true));
  }

  @Test
  public final void wildcardPatternMatchingGuarded3() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("count", "infinity");

    assertEquals(map, Loop.run("test/loop/confidence/wildcard_pattern_matching_guarded_3.loop", true));
  }

  @Test
  public final void regexPatternMatching() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "Michael");
    map.put("age", "212");

    assertEquals(map, Loop.run("test/loop/confidence/regex_pattern_matching.loop", true));
  }

  @Test
  public final void regexPatternMatchingGuarded1() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "Dhanji");
    map.put("age", "20");

    assertEquals(map, Loop.run("test/loop/confidence/regex_pattern_matching_guarded_1.loop", true));
  }

  @Test
  public final void regexPatternMatchingGuarded2() {
    Map<String, String> map = new HashMap<String, String>();

    assertEquals(map, Loop.run("test/loop/confidence/regex_pattern_matching_guarded_2.loop", true));
  }

  @Test
  public final void regexPatternMatchingGuarded3() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "Unknown");
    map.put("age", "-1");

    assertEquals(map, Loop.run("test/loop/confidence/regex_pattern_matching_guarded_3.loop", true));
  }
}
