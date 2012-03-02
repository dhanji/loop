package loop;

import loop.ast.script.Unit;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class AsmCodeEmitterTest {
  @After
  public void tearDown() throws Exception {
    LoopClassLoader.reset();
  }

  @Test
  public final void emitBasicCall()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Parser parser = new Parser(new Tokenizer("puts ->\n  'HELLO'.toLowerCase()").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("puts").invoke(null));
  }

  @Test
  public final void emitBasicCallWithArgs()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Parser parser = new Parser(new Tokenizer("puts(str) ->\n  str.toLowerCase()").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("puts", Object.class).invoke(null, "HELLO"));
  }

  @Test
  public final void emitCallLoopFunctionWithArgs()
      throws Exception, InvocationTargetException, IllegalAccessException {
    Parser parser = new Parser(new Tokenizer("puts(str) ->\n  str.toLowerCase()\n\nmain() ->\n  puts('HELLO')\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("main").invoke(null));
  }

  private static void inspect(Class<?> generated) {
    System.out.println(generated);
    System.out.println("Fields:");
    for (Field field : generated.getDeclaredFields()) {
      System.out.println("  " + field);
    }
    System.out.println("Methods:");
    for (Method method : generated.getDeclaredMethods()) {
      System.out.println("  " + method + " " + (Modifier.isStatic(method.getModifiers()) ? "static" : ""));
    }
  }
}
