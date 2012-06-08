package loop.confidence.interop;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import loop.Loop;

import loop.LoopTest;

/**
 * Confidence tests for Loop.implement(SomeInterface.class).
 *
 * @author galdolber
 */
public class JavaInterfaceConfidenceTest extends LoopTest {
  public static final String LOOP_IMPL = "test/loop/confidence/interop/java_interface";
  private ExampleJavaInterface i;

  @Before
  public void before() {
    i = Loop.implement(ExampleJavaInterface.class, LOOP_IMPL);
  }

  @Test(expected = RuntimeException.class)
  public void loopImplAClass() {
    Loop.implement(JavaInterfaceConfidenceTest.class, LOOP_IMPL);
  }

  @Test
  public void noArgsMethod() {
    assertEquals("Hello", i.noArgumentsMethod());
  }

  @Test(expected = RuntimeException.class)
  public void methodNotImplemented() {
    i.unexistingMethod();
  }

  @Test
  public void callSimpleMethodWithPrimitiveArgs() {
    assertEquals(20.0, i.multiply(2.0, 10.0), 0.0);
  }

  @Test
  public void callPolymorphicMethodImplementedWithPatternMatching() {
    assertEquals("Hello John", i.sayHello("John"));
    assertEquals("Hello Doe", i.sayHello(new Person("Doe")));
  }
}
