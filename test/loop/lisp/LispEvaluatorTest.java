package loop.lisp;

import loop.Loop;
import loop.ast.script.ModuleLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;

/**
 * IO a -> IO b -> IO ()
 * IO a -> a    X(
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LispEvaluatorTest {
  @Before
  public void pre() {
    ModuleLoader.reset();
  }

  @Test
  public final void evalLisp() {
    String lisp =
        "((define moon () (print (print (print ('hello there')))))" +
        " (define sun () (moon))" +
        " (sun))";
    Loop.evalLisp("default", new StringReader(lisp));
  }

  @Test
  public final void evalLispExpressions() {
    String lisp =
        "((print (+ 1 2 3 5 9))" +
            ")";
    Loop.evalLisp("default", new StringReader(lisp));
  }

  @Test
  public final void evalLispFile() {
//    Loop.evalLisp("default", new InputStreamReader(LispEvaluatorTest.class.getResourceAsStream("funcs.lisp")));
  }
}
