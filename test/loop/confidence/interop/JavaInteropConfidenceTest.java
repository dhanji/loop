package loop.confidence.interop;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class JavaInteropConfidenceTest extends LoopTest {
  public static final Integer CONSTANT = Long.valueOf(new Date().getTime()).intValue();

  @Test
  public final void newInstanceByReflection() {
    assertEquals("",
        Loop.run("test/loop/confidence/interop/new_instance.loop"));
  }

  @Test
  public final void newInstanceByReflectionWithForName() {
    assertEquals("",
        Loop.run("test/loop/confidence/interop/new_instance_2.loop"));
  }

  @Test
  public final void newInstanceByReflectionWithForNameCalendar() {
    Date now = new Date();
    Object result = Loop.run("test/loop/confidence/interop/new_instance_3.loop");
    assertTrue(result instanceof Date);
    assertTrue(now.before((Date) result) || now.equals(result));
  }

  @Test
  public final void newInstanceByReflectionCalendar() {
    Date now = new Date();
    Object result = Loop.run("test/loop/confidence/interop/new_instance_4.loop");
    assertTrue(result instanceof Date);
    assertTrue(now.before((Date) result) || now.equals(result));
  }

  @Test
  public final void mainMethodArgs() {
    assertEquals(Arrays.asList("ARG1", "ARG2"),
        Loop.run("test/loop/confidence/interop/main_args.loop", new String[] { "arg1", "arg2" }));
  }

  @Test
  public final void normalFunctionCall() {
    assertEquals("hello", Loop.run("test/loop/confidence/interop/postfix_call_1.loop"));
  }

  @Test
  public final void forceCallAsJava() {
    assertEquals("hello", Loop.run("test/loop/confidence/interop/postfix_call_2.loop"));
  }

  @Test
  public final void callOverloadedJavaMethod() {
    assertEquals("hello", Loop.run("test/loop/confidence/interop/overloaded_call_1.loop"));
  }

  @Test
  public final void callAsLoopOverridingJava() {
    assertEquals("HELLO", Loop.run("test/loop/confidence/interop/postfix_call_3.loop"));
  }

  @Test
  public final void callJavaStaticFunction() {
    assertTrue(1327205727145L /* epoch time when this test was written */ < Long.parseLong(
        Loop.run("test/loop/confidence/interop/call_java_static.loop").toString()));
  }

  @Test
  public final void callJavaStaticConstant() {
    assertEquals(CONSTANT + 1, Loop.run("test/loop/confidence/interop/call_java_static_2.loop"));
  }
}
