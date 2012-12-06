package loop.lisp;

import loop.LexprParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class SexprParserTest {
  @Test
  public final void sExpressions() {
    check("(list (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx))", "(xx)");
    check("(list (list (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx)))", "((xx))");
    check("(list (comput 1 (+ 2) (+ 3) (+ 4)))", "(+ 1 2 3 4)");
    check("(list (comput (list 1) (+ 2) (+ 3) (+ 4)))", "(+ (1) 2 3 4)");
    check("(list (comput (list 1) (+ 2) (+ list) (+ 3) (+ 4)))", "(+ (1) 2 () 3 4)");
    check("(list (comput (list 1) (+ 2) (+ (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx)) (+ 3) (+ 4)))", "(+ (1) 2 (xx) 3 4)");
    check("(list (comput (list 1) (+ 2) (+ (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx)) (+ 3) (+ 4)))", "(+ (1\n) \n2 (\nxx\n\n )       3 4)");
    check("(list (comput (list 1) (+ 2) (+ (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx)) (+ 3) (+ 4)))", "\n\n\n  (+ (1\n) \n2 (\nxx\n\n )       3 4)\n\n");
    check("(list (comput (list 1) (+ 2) (+ (if-then-else (comput xx (in `loop.runtime.Closure`)) xx() xx)) (+ 3) (+ 4)))", "\n\n; hello\n  (+ (1 ;crud\n) \n2 (\nxx\n\n )       3 4)\n\n");
  }

  public static void check(String expected, String input) {
    SexprParser sexprParser = new SexprParser(new SexprTokenizer(input).tokenize());

    String stringified = LexprParser.stringify(sexprParser.parse());
    assertEquals(stringified, expected, stringified);
  }
}
