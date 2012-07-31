package loop;

import loop.ast.script.Unit;
import org.junit.Test;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.VariableSpaceCompiler;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Measures the performance of various Loop scripts compiled against the MVEL and
 * ASM/Java Bytecode backends.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings("unchecked") // No idea why but the compiler keeps whinging
public class AsmMvelPerformanceBenchmark extends LoopTest {
  // Number of cycles for the benchmark, should be > 200000 for anything useful.
//  private static final int RUNS = 500000;
  private static final int RUNS = 1;
  private static final int WARMUP_RUNS = RUNS > 100 ? 15000 : 0;

  @Test
  public final void quicksort() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("quicksort", Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke("quicksort", Arrays.asList(5, 2, 6, 19, 0, 92, 144, 1));
      }
    };

    time("quicksort(ls) =>\n" +
        "  []        : []\n" +
        "  [x:xs]    : quicksort(i for i in xs if i < x) + [x] + quicksort(i for i in xs if i > x)\n" +
        "\n",
        callable, "quicksort([5, 2, 6, 19, 0, 92, 144, 1])");
  }


//  @Test
  public final void interpolatedStrings() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("fun", Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, "yo");
      }
    };

    time("fun(name) ->\n  \"hhi @{name.toLowerCase()} - @{1 + 2} >>@{5 * 4}<< @{name} @{name} \"",
        callable, "fun('yo');");
  }


  @Test
  public final void patternMatchingFunctionWithGuards() throws Exception {
    String script =
        "pick(ls) =>\n" +
            "  5                    : 'five'\n" +
            "  *         | ls == 1  : 'one'\n" +
            "            | ls == 2  : 'two'\n" +
            "            | else     : 'other'\n";

    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("pick", Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, 2);
      }
    };

    time(script, callable, "pick(2);");
  }


  @Test
  public final void newJavaObject() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("fun");
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null);
      }
    };

    time("fun() ->\n  new loop.TestObjectWithUnaryCtor(23)", callable, "fun();");
  }

  @Test
  public final void newJavaObjectWithNullaryCtor() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("fun");
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null);
      }
    };

    time("fun() ->\n  new java.lang.String()", callable, "fun();");
  }

  @Test
  public final void valueTypedEquals() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("eq", Object.class, Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, "mynameisinigomontoya", "mynameisinigomontoya");
      }
    };

    time("eq(m1, m2) ->\n  m1 == m2", callable, "eq(\"mynameisinigomontoya\", \"mynameisinigomontoya\");");
  }

  @Test
  public final void loopFunctionCalling() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("func");
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null);
      }
    };

    time("cons(x) ->\n  '1'\n\nfunc() ->\n  cons('10')", callable, "func();");
  }

  @Test
  public final void loopFunctionCallingWithIntegrals() throws Exception {
    Callable callable = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("func");
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null);
      }
    };

    time("cons(x) ->\n  1 + 2\n\nfunc() ->\n  cons(10)", callable, "func();");
  }

  @Test
  public final void integerAddition() throws Exception {
    Callable add = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("add", Object.class, Object.class, Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, 10, 20, 40);
      }
    };

    time("add(x, y, z) ->\n  x + y + z\n", add, "add(10, 20, 40);");
  }

  @Test
  public final void arithmetic() throws Exception {
    Callable add = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("arith", Object.class, Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, 10, 20);
      }
    };

    time("arith(x, y) ->\n  ((100 + x - y) * 10 / 2) % 40\n", add, "arith(10, 20);");
  }

  @Test
  public final void collectionAddition() throws Exception {
    Callable add = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("add", Object.class, Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, Arrays.asList(10, 20), Arrays.asList(40));
      }
    };

    time("add(x, y) ->\n  x + y\n", add, "add([10, 20], [40]);");
  }

  public static void time(String script, Callable javaCallable, String mvelCallable) throws Exception {
    if (RUNS > 1) {
      System.out.println("Profiling loop script:");
      System.out.println(script);
      System.out.println("\n\n");
    }
    Parser parser = new Parser(new Tokenizer(script).tokenize());
    Unit unit = parser.script(null);
    unit.reduceAll();

    // Compile ASM.
    Class<?> generated = new AsmCodeEmitter(unit).write(unit);
    Method asmCallable = javaCallable.lookup(generated);

    // Compile MVEL.
    String mvel = new MvelCodeEmitter(unit).write(unit);
    Serializable compiledMvel = MVEL.compileExpression(mvel + "; " + mvelCallable);

    // Assert validity.
    Object javaGen = javaCallable.call(asmCallable);
    Object mvelGen = MVEL.executeExpression(compiledMvel, new HashMap());
    assertNotNull(javaGen);
    assertNotNull(mvelGen);

    // MVEL incorrectly type-widens certain integer arithmetic expressions to double. If so,
    // convert it back into an integer.
    if (mvelGen instanceof Double && !(javaGen instanceof Double))
      mvelGen = ((Double)mvelGen).intValue();

    assertEquals(javaGen, mvelGen);

    // Warm up JVM.
    for (int i = 0; i < WARMUP_RUNS; i++) {
       javaCallable.call(asmCallable);
       MVEL.executeExpression(compiledMvel, new HashMap());
    }

    String targetRunMvel = mvel + ";\n for(i = 0; i < " + RUNS + "; i++) {\n "
        + mvelCallable + "\n}\n";

    ParserContext parserContext = new ParserContext();
    parserContext.addIndexedInput(new String[0]);

    VariableResolverFactory factory =
        VariableSpaceCompiler.compile(targetRunMvel, parserContext).createFactory(new Object[0]);
    compiledMvel = MVEL.compileExpression(targetRunMvel, parserContext);

    long start = System.currentTimeMillis();
    MVEL.executeExpression(compiledMvel, factory);

    if (RUNS > 1)
      System.out.println("Mvel runtime: " + (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < RUNS; i++) {
      javaCallable.call(asmCallable);
    }
    if (RUNS > 1) {
      System.out.println("Asm runtime: " + (System.currentTimeMillis() - start));
      System.out.println();
    }
  }

  public static interface Callable {
    Method lookup(Class target) throws Exception;
    Object call(Method target) throws Exception;
  }
}
