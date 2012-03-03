package loop;

import loop.ast.script.Unit;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
  public final void emitCallLoopFunctionWithArgs() throws Exception {
    Parser parser = new Parser(new Tokenizer("puts(str) ->\n  str.toLowerCase()\n\nmain() ->\n  puts('HELLO')\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitCallLoopFunctionWithPrimitives() throws Exception {
    Parser parser = new Parser(new Tokenizer("puts(num) ->\n  num\n\nmain() ->\n  puts(20)\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(20, generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitNumericAddition() throws Exception {
    Parser parser = new Parser(new Tokenizer("add(x, y) ->\n  x + y\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(30, generated.getDeclaredMethod("add", Object.class, Object.class).invoke(null, 10, 20));
  }


  @Test
  public final void emitNumericSubtraction() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  x - y\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(-10, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, 10, 20));
  }

  @Test
  public final void emitNumericArithmetic() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  ((100 + x - y) * 10 / 2) % 40\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(10, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, 10, 20));
  }


  @Test
  public final void emitEquals() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  x == y\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(true, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, "hi", "hi"));
  }


  @Test
  public final void emitInlineListDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  [1, 2, 3]\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(1,2,3), generated.getDeclaredMethod("fun").invoke(null));
  }

  @Test
  public final void emitInlineSetDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  {1, 2, 3}\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(new HashSet<Integer>(Arrays.asList(1,2,3)), generated.getDeclaredMethod("fun").invoke(null));
  }


  @Test
  public final void emitIndexIntoList() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1]\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(2, generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitIndexIntoList2() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1..3]\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(2, 3), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }

  @Test
  public final void emitIndexIntoList3() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1..]\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(2, 3), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitIndexIntoList4() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[..2]\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(1, 2), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitInlineMapDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  [1: 'a', 2: 'b', 3: 'c']\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Map<Integer, String> map = new HashMap<Integer, String>();
    map.put(1, "a");
    map.put(2, "b");
    map.put(3, "c");
    assertEquals(map, generated.getDeclaredMethod("fun").invoke(null));
  }


  @Test
  public final void emitListAddition() throws Exception {
    Parser parser = new Parser(new Tokenizer("add(x, y) ->\n  x + y\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(10, 20), generated.getDeclaredMethod("add", Object.class, Object.class)
        .invoke(null, Arrays.asList(10), Arrays.asList(20)));
  }


  @Test
  public final void emitListComprehension() throws Exception {
    Parser parser = new Parser(new Tokenizer("sum(ls) ->\n  i for i in ls if i < 25\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(10, 20), generated.getDeclaredMethod("sum", Object.class)
        .invoke(null, Arrays.asList(10, 20, 30, 40)));
  }


  @Test
  public final void emitIfStatement() throws Exception {
    Parser parser = new Parser(new Tokenizer("sum(cond) ->\n  if cond then 1 else 2\n").tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit, true);

    // Inspect.
    inspect(generated);

    assertEquals(2, generated.getDeclaredMethod("sum", Object.class).invoke(null));
  }

  private static void inspect(Class<?> generated) {
    System.out.println(generated);
    System.out.println("Fields:");
    for (Field field : generated.getDeclaredFields()) {
      System.out.println("  " + field);
    }
    System.out.println("Methods:");
    for (Method method : generated.getDeclaredMethods()) {
      System.out.println("  " + method + " ");
    }
  }
}
