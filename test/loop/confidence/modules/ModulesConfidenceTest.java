package loop.confidence.modules;

import loop.AnnotatedError;
import loop.Loop;
import loop.LoopCompileException;
import loop.LoopTest;
import loop.ast.script.ModuleLoader;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ModulesConfidenceTest extends LoopTest {
  @Test(expected = LoopCompileException.class)
  public final void requireFaultyLoopModule() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_loop_error_1.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopModuleTwiceCausesVerifyError() {
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(10, Loop.run("test/loop/confidence/modules/require_loop_error_2.loop"));
  }

  @Test
  public final void requireLoopModule() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(30, Loop.run("test/loop/confidence/modules/require_loop_mod_1.loop"));
  }

  @Test
  public final void requireAliasedLoopModule() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals("hi", Loop.run("test/loop/confidence/modules/require_loop_mod_5.loop"));
  }

  @Test
  public final void requireLoopModuleTransitivelyErrorsIfMissingModuleDecl() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    List<AnnotatedError> errors = null;
    try {
      Loop.run("test/loop/confidence/modules/require_loop_mod_3.loop");
      fail();
    } catch (LoopCompileException e) {
      errors = e.getErrors();
    }

    assertNotNull(errors);
    assertEquals(1, errors.size());
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopModuleHidesTransitiveDeps() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(4, Loop.run("test/loop/confidence/modules/require_loop_mod_4.loop"));
  }

  @Test
  public final void requireLoopModuleRaiseAndCatchException() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals("now is the winter of our discontent!",
        Loop.run("test/loop/confidence/modules/require_loop_mod_2.loop"));
  }

  @Test
  public final void preludeConfidence1() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(true,
        Loop.run("test/loop/confidence/modules/prelude_conf_1.loop"));
  }

  @Test
  public final void requireJavaClass() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java.loop"));
  }

  @Test
  public final void requireLoopClass() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("left", 1);
    map.put("right", 2);

    assertEquals(map, Loop.run("test/loop/confidence/modules/require_class.loop"));
  }

  @Test
  public final void requireLoopClassWithAlias() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("left", 10);
    map.put("right", 20);

    assertEquals(map, Loop.run("test/loop/confidence/modules/require_class_3.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopClassWithAliasFails() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("left", 10);
    map.put("right", 20);

    assertEquals(map, Loop.run("test/loop/confidence/modules/require_class_4.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopClassWithAliasFails2() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("left", 10);
    map.put("right", 20);

    assertEquals(map, Loop.run("test/loop/confidence/modules/require_class_5.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopClassHidesTransitiveDeps() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("left", 1);
    map.put("right", 2);

    assertEquals(map, Loop.run("test/loop/confidence/modules/require_class_2.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireJavaClassFails() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java_error.loop"));
  }

  @Test
  public final void requiredModuleHidesPrivateFunctions() {
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals("1", Loop.run("test/loop/confidence/modules/require_hides_private.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requiredModuleHidesPrivateFunctionsError() {
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals("1", Loop.run("test/loop/confidence/modules/require_hides_private_err.loop"));
  }

  @Test
  public final void requireFileModule() {
    assertTrue(Loop.run("test/loop/confidence/modules/require_file.loop").toString().contains("http://looplang.org"));
  }
}
