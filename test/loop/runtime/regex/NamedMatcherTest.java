package loop.runtime.regex;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class NamedMatcherTest {
  @Test
  public final void namedCapturingGroups() {
    NamedMatcher matcher = NamedPattern.compile("hello\n(?<name>.*)\n").matcher("hello\nDhanji\n");
    assertTrue(matcher.matches());
    assertEquals("Dhanji", matcher.group("name"));
  }

  @Test
  public final void namedCapturingGroups2() {
    NamedMatcher matcher = NamedPattern.compile("(?<one>hello)\n(?<two>there)\n(?<three>dude)")
        .matcher("hello\nthere\ndude");
    assertTrue(matcher.matches());
    assertEquals("hello", matcher.group("one"));
  }
}
