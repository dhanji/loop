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
public class SyntaxErrorsConfidenceTest {
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
  public final void patternMatchingFunctionErrors4() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_4.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors5() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_5.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors6() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_6.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors7() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_7.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void patternMatchingFunctionErrors8() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/pattern_errors_8.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
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

  @Test
  public final void listOrMapErrors3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/list_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listOrMapErrors4() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/list_errors_4.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listOrMapErrors5() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/list_errors_5.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void functionDeclErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void functionDeclErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void functionDeclErrors3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors4() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_4.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors5() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_5.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void moduleErrors6() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/module_errors_6.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void incompleteVariableAssignment() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/assign_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void ifThenErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/ifthen_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void ifThenErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/ifthen_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listComprehension1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/comp_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listComprehension2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/comp_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listComprehension3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/comp_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void listComprehension4() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/comp_errors_4.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void groupErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/group_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void groupErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/group_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void funcCallErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_call_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void funcCallErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_call_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void funcCallErrors3() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/func_call_errors_3.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void indexIntoErrors1() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/index_errors_1.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }

  @Test
  public final void indexIntoErrors2() {
    List<ParseError> errorList = null;
    try {
      Loop.run("test/loop/confidence/errors/index_errors_2.loop");
    } catch (LoopSyntaxException e) {
      errorList = e.getErrors();
    }

    assertNotNull(errorList);

    assertEquals(1, errorList.size());
  }
}
