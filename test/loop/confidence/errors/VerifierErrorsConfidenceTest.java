package loop.confidence.errors;

import loop.AnnotatedError;
import loop.Loop;
import loop.LoopCompileException;
import loop.LoopTest;
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
public class VerifierErrorsConfidenceTest extends LoopTest {
  @Test
  public final void verifyUnknownFunctionCall() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_1.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }
  @Test
  public final void verifyIllegalArgumentReassignment() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/assign_errors_2.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void verifyUnknownFunctionCallInWhereBlock() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_2.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(3, errorList.size());
  }

  @Test
  public final void verifyFunctionCallWithIncorrectArgLen() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_3.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(3, errorList.size());
  }

  @Test
  public final void verifyFunctionCallWithNesting() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_4.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(2, errorList.size());
  }

  @Test
  public final void verifyJavaConstructorCall() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_5.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(3, errorList.size());
  }

  @Test
  public final void verifyPatternMatchingFunc() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_6.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(3, errorList.size());
  }

  @Test
  public final void verifyDuplicateFunctions() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_7.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void verifyDuplicateTypes() {
    List<AnnotatedError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/verify_error_8.loop");
    } catch (LoopCompileException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }
}
