package loop;

import org.junit.Test;

import static loop.ParserTest.compare;

/**
 * NOTE: Pattern matching functions all start with => rather than ->
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class PatternMatchingFunctionsParsingTest {

  @Test
  public final void reverseListInPatternMatchingForm() {
    compare("(comput (. a b c))", "reverse(list) =>\n");
  }
}
