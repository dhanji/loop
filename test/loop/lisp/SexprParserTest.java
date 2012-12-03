package loop.lisp;

import loop.LexprParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class SexprParserTest {
  @Test
  public final void someExpressions() {
    check("(list xx)", "(xx)");
    check("(list (list xx))", "((xx))");
    check("(list + 1 2 3 4)", "(+ 1 2 3 4)");
    check("(list + (list 1) 2 3 4)", "(+ (1) 2 3 4)");
    check("(list + (list 1) 2 list 3 4)", "(+ (1) 2 () 3 4)");
    check("(list + (list 1) 2 (list xx) 3 4)", "(+ (1) 2 (xx) 3 4)");
    check("(list + (list 1) 2 (list xx) 3 4)", "(+ (1\n) \n2 (\nxx\n\n )       3 4)");
  }

  public static void check(String expected, String input) {
    SexprParser sexprParser = new SexprParser(new SexprTokenizer(input).tokenize());

    String stringified = LexprParser.stringify(sexprParser.parse());
    assertEquals(stringified, expected, stringified);
  }
}
