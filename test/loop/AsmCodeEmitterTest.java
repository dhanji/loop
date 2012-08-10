package loop;

import loop.ast.script.Unit;
import loop.lang.LoopObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class AsmCodeEmitterTest extends LoopTest {
  private String file = null;

  @Test
  public final void emitBasicCall()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Parser parser = new Parser(new Tokenizer("puts ->\n  'HELLO'.toLowerCase()").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("puts").invoke(null));
  }

  @Test
  public final void emitBasicCallWithArgs()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Parser parser = new Parser(new Tokenizer("puts(str) ->\n  str.toLowerCase().toUpperCase().toLowerCase()").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("puts", Object.class).invoke(null, "HELLO"));
  }

  @Test
  public final void emitCallLoopFunctionWithArgs() throws Exception {
    Parser parser = new Parser(new Tokenizer("puts(str) ->\n  str.toLowerCase()\n\nmain() ->\n  puts('HELLO')\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitCallLoopFunctionWithPrimitives() throws Exception {
    Parser parser = new Parser(new Tokenizer("puts(num) ->\n  num\n\nmain() ->\n  puts(20)\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(20, generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitNumericAddition() throws Exception {
    Parser parser = new Parser(new Tokenizer("add(x, y) ->\n  x + y\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(30, generated.getDeclaredMethod("add", Object.class, Object.class).invoke(null, 10, 20));
  }


  @Test
  public final void emitNumericSubtraction() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  x - y\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(-10, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, 10, 20));
  }

  @Test
  public final void emitNumericArithmetic() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  ((100 + x - y) * 10 / 2) % 40\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(10, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, 10, 20));
  }


  @Test
  public final void emitEquals() throws Exception {
    Parser parser = new Parser(new Tokenizer("sub(x, y) ->\n  x == y\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(true, generated.getDeclaredMethod("sub", Object.class, Object.class).invoke(null, "hi", "hi"));
  }


  @Test
  public final void emitInlineListDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  [1, 2, 3]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(1,2,3), generated.getDeclaredMethod("fun").invoke(null));
  }

  @Test
  public final void emitInlineSetDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  {1, 2, 3}\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(new HashSet<Integer>(Arrays.asList(1,2,3)), generated.getDeclaredMethod("fun").invoke(null));
  }


  @Test
  public final void emitIndexIntoList() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(2, generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitIndexIntoList2() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1..3]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(2, 3, 4), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3, 4)));
  }

  @Test
  public final void emitIndexIntoList3() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[1..]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(2, 3), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitIndexIntoList4() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(ls) ->\n  ls[..2]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(1, 2, 3), generated.getDeclaredMethod("fun", Object.class).invoke(null, Arrays.asList(1, 2, 3)));
  }


  @Test
  public final void emitInlineMapDef() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun() ->\n  [1: 'a', 2: 'b', 3: 'c']\n").tokenize());
    Unit unit = parser.script(file);
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
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(10, 20), generated.getDeclaredMethod("add", Object.class, Object.class)
        .invoke(null, Arrays.asList(10), Arrays.asList(20)));
  }


  @Test
  public final void emitStringSlice() throws Exception {
    Parser parser = new Parser(new Tokenizer("slice(str) ->\n  str[1..3]\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("ell", generated.getDeclaredMethod("slice", Object.class)
        .invoke(null, "Hello"));
  }



  @Test
  public final void emitLiteralPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "pick(ls) =>\n" +
        "  1         : 'one'\n" +
        "  2         : 'two'\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("two", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, 2));
  }


  @Test
  public final void emitWhereBlockFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "pick(ls) =>\n" +
        "  1         : 'one'\n" +
        "  2         : two()\n" +
        "  where\n" +
        "    two ->\n" +
        "      two()\n" +
        "    where\n" +
        "      two ->\n" +
        "        'two'\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("two", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, 2));
  }


  @Test
  public final void emitPrivateFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "pick(ls) =>\n" +
        "  1         : 'one'\n" +
        "  2         : @two()\n" +
        "\n" +
        "@two() ->\n" +
        "  'two'\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("two", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, 2));
  }



  @Test
  public final void emitStringPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "pick(str) =>\n" +
        "  (a: '--' :b)         : a + b\n" +
        "  ''                   : ''\n" +
        "  *                    | str == 'yoyo'  : 'ma'\n" +
        "                       | else           : 'nothing'\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("abcc", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, "ab--cc"));
    assertEquals("ma", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, "yoyo"));
    assertEquals("", generated.getDeclaredMethod("pick", Object.class)
        .invoke(null, ""));
  }


  @Test
  public final void emitPatternMatchingFunctionWithGuards() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "pick(ls) =>\n" +
        "  5                    : 'five'\n" +
        "  *         | ls == 1  : 'one'\n" +
        "            | ls == 2  : 'two'\n" +
        "            | else     : 'other'\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Method pick = generated.getDeclaredMethod("pick", Object.class);
    assertEquals("two", pick.invoke(null, 2));
    assertEquals("one", pick.invoke(null, 1));
    assertEquals("five", pick.invoke(null, 5));
    assertEquals("other", pick.invoke(null, 434));
  }


  @Test
  public final void emitListPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "reverse(ls) =>\n" +
        "  []         : []\n" +
        "  [x:xs]     : reverse(xs) + [x]\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(6, 5, 4, 3, 2, 1), generated.getDeclaredMethod("reverse", Object.class)
        .invoke(null, Arrays.asList(1, 2, 3, 4, 5, 6)));
  }

  @Test
  public final void emitListStructurePatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "reverse(ls) =>\n" +
        "  []                 : []\n" +
        "  [x]                : [x]\n" +
        "  [one, two] | true  : [two, one]\n" +
        "             | else  : []\n" +
        "  [x:xs]             : reverse(xs) + [x]\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(Arrays.asList(6, 5, 4, 3, 2, 1), generated.getDeclaredMethod("reverse", Object.class)
        .invoke(null, Arrays.asList(1, 2, 3, 4, 5, 6)));
  }


  @Test
  public final void emitMapPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) =>\n" +
        "  [ x <- obj.name]         : x.toLowerCase()\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Map<String, String> obj = new HashMap<String, String>();
    obj.put("name", "Dude");

    assertEquals("dude", generated.getDeclaredMethod("lower", Object.class).invoke(null, obj));
  }


  @Test
  public final void emitTypedMapPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "require `java.util.List`\n" +
        "require `java.util.Map`\n" +
        "\n" +
        "lower(obj) =>\n" +
        "  List[ x <- obj.name]        : x.toUpperCase()\n" +
        "  Map[ x <- obj.name]         : x.toLowerCase()\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Map<String, String> obj = new HashMap<String, String>();
    obj.put("name", "Dude");

    assertEquals("dude", generated.getDeclaredMethod("lower", Object.class).invoke(null, obj));
  }



  @Test
  public final void emitTypedPatternMatchingFunction() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "require `java.util.List`\n" +
        "require `java.util.Map`\n" +
        "\n" +
        "lower(obj) =>\n" +
        "  List        : obj.name.toUpperCase()\n" +
        "  Map         : obj.name.toLowerCase()\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Map<String, String> obj = new HashMap<String, String>();
    obj.put("name", "Dude");

    assertEquals("dude", generated.getDeclaredMethod("lower", Object.class).invoke(null, obj));
  }



  @Test
  public final void emitNullaryClosure() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) ->\n" +
        "  obj.@call().toUpperCase()\n" +
        "\n" +
        "main ->\n" +
        "  lower(@() ->\n" +
        "          'two')\n" +
        "\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("TWO", generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitBinaryClosure() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) ->\n" +
        "  obj.@call(1, 2) * 10\n" +
        "\n" +
        "main ->\n" +
        "  lower(@(a, b) ->\n" +
        "          a + b)\n" +
        "\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(30, generated.getDeclaredMethod("main").invoke(null));
  }



  @Test
  public final void emitBinaryClosureWithFreeVars() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) ->\n" +
        "  obj.@call(1, 2) * 10\n" +
        "\n" +
        "main ->\n" +
        "  c: 4\n" +
        "  lower(@(a, b) ->\n" +
        "          a + b + c)\n" +
        "\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(70, generated.getDeclaredMethod("main").invoke(null));
  }

  @Test
  public final void emitCallAsMethod() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) ->\n" +
        "  obj.toLowerCase()\n" +
        "\n" +
        "main ->\n" +
        "  'HELLO'.lower()\n" +
        "\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("hello", generated.getDeclaredMethod("main").invoke(null));
  }


  @Test
  public final void emitNullSafeCalls() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "lower(obj) ->\n" +
        "  obj.toLowerCase().toLowerCase()\n" +
        "\n"
    ).tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(null, generated.getDeclaredMethod("lower", Object.class).invoke(null, new Object[] { null }));
  }


  @Test
  public final void emitWhereBlock() throws Exception {
    Parser parser = new Parser(new Tokenizer("compute() ->\n" +
        "  day: 24\n" +
        "  week: 7 * day\n" +
        "  year: 52 * week\n" +
        "  3 * year\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(26208, generated.getDeclaredMethod("compute")
        .invoke(null));
  }


  @Test
  public final void emitMapIndexInto() throws Exception {
    Parser parser = new Parser(new Tokenizer("slice(map) ->\n  map['num']\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Map<String, Integer> map = new HashMap<String, Integer>();
    map.put("num", 22);

    assertEquals(22, generated.getDeclaredMethod("slice", Object.class)
        .invoke(null, map));
  }


  @Test
  public final void emitInterpolatedString() throws Exception {
    Parser parser = new Parser(new Tokenizer("fun(name) ->\n  \"Hi, @{name.toUpperCase()}! @{name.toLowerCase()}\"\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals("Hi, DHANJI! dhanji", generated.getDeclaredMethod("fun", Object.class).invoke(null, "Dhanji"));

  }

  @Test
  public final void emitJavaConstructor() throws Exception {
    Parser parser = new Parser(new Tokenizer("main() ->\n  new java.util.Date(1)\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Object date = generated.getDeclaredMethod("main").invoke(null);
    assertTrue(date instanceof java.util.Date);
    assertTrue(new Date(1).equals(date));
  }

  @Test
  public final void emitLoopConstructor() throws Exception {
    Parser parser = new Parser(new Tokenizer(
        "class Star ->\n" +
        "  name\n" +
        "  galaxy: 'Andromeda'\n" +
        "  mass\n" +
        "  nebula: false\n" +
        "\n" +
        "main() ->\n" +
        "  new Star(name: 'Proxima', mass: 123)\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Object starObject = generated.getDeclaredMethod("main").invoke(null);
    assertTrue(starObject instanceof LoopObject);
    LoopObject star = (LoopObject) starObject;
    assertEquals("Star", star.getType().name);

    LoopObject expected = new LoopObject(star.getType());
    expected.put("name", "Proxima");
    expected.put("mass", 123);
    expected.put("nebula", false);
    expected.put("galaxy", "Andromeda");

    assertEquals(expected, star);
  }


  @Test
  public final void emitJavaNullaryConstructor() throws Exception {
    Parser parser = new Parser(new Tokenizer("main() ->\n  new java.util.Date()\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Object date = generated.getDeclaredMethod("main").invoke(null);
    assertTrue(date instanceof java.util.Date);
  }


  @Test
  public final void emitJavaBeanPropertyCall() throws Exception {
    Parser parser = new Parser(new Tokenizer("main() ->\n  new java.util.Date(1).time\n").tokenize());
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    Object out = generated.getDeclaredMethod("main").invoke(null);
    assertEquals(1L, out);
  }


  @Test
  public final void emitListComprehension() throws Exception {
    Parser parser = new Parser(new Tokenizer("sum(ls) ->\n  i for i in ls if i < 25\n").tokenize());
    Unit unit = parser.script(file);
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
    Unit unit = parser.script(file);
    unit.reduceAll();

    Class<?> generated = new AsmCodeEmitter(unit).write(unit);

    // Inspect.
    inspect(generated);

    assertEquals(2, generated.getDeclaredMethod("sum", Object.class).invoke(null, false));
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
