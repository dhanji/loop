package loop.confidence.interop;

import loop.Loop;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class JavaInteropConfidenceTest {
  public static final Long CONSTANT = new Date().getTime();

  @Test
  public final void normalFunctionCall() {
    assertEquals("hello", Loop.run("test/loop/confidence/interop/postfix_call_1.loop"));
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
