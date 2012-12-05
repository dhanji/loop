package loop.lisp;

import loop.Loop;
import org.junit.Test;

import java.io.StringReader;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LispEvaluatorTest {
  @Test
  public final void evalLisp() {
    String lisp =
        "((define moon () (print (print (print ('hello there')))))" +
        " (moon)" +
        " (moon)" +
        " (moon))";
    Loop.evalLisp("default", new StringReader(lisp));
  }
}
