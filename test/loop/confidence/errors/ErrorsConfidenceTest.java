package loop.confidence.errors;

import loop.Loop;
import loop.LoopSyntaxException;
import loop.ParseError;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ErrorsConfidenceTest {
  @Test
  public final void patternMatchingFunctionErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(2, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(2, errorList.size());
  }

  @Test
  public final void listOrMapErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/list_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listOrMapErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/list_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }
}
