package loop.confidence.interop;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import loop.Loop;

import loop.LoopTest;

/**
 * Confidence tests for Loop.get(SomeInterface.class).
 */
public class JavaInterfaceConfidenceTest extends LoopTest {

  LoopInterface i;

  @Before
  public void before() {
    i = Loop.get(LoopInterface.class);
  }

  @Test(expected = RuntimeException.class)
  public void loopGetAClass() {
    Loop.get(JavaInterfaceConfidenceTest.class);
  }

  @Test
  public void noArgsMethod() {
    assertEquals("Hello", i.noArgumentsMethod());
  }

  @Test(expected = RuntimeException.class)
  public void noMethodException() {
    i.unexistingMethod();
  }

  @Test
  public void callSimpleMethod() {
    assertEquals(20.0, i.multiply(2.0, 10.0), 0.0);
  }

  @Test
  public void callPolymorphicMethod() {
    assertEquals("Hello John", i.sayHello("John"));
    assertEquals("Hello Doe", i.sayHello(new Person("Doe")));
  }
}
