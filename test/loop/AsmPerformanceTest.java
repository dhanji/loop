package loop;

import loop.ast.script.Unit;
import org.junit.Test;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Measures the performance of various Loop scripts compiled against the MVEL and
 * ASM/Java Bytecode targets.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class AsmPerformanceTest {

  private static final int RUNS = 500000;

  @Test
  public final void integerAddition() throws Exception {
    Callable add = new Callable() {

      @Override public Method lookup(Class target) throws Exception {
        return target.getDeclaredMethod("add", Object.class, Object.class);
      }

      @Override public Object call(Method target) throws Exception {
        return target.invoke(null, 10, 20);
      }
    };

    time("add(x, y) ->\n  x + y\n", add, "add(10, 20);");
  }

  public static void time(String script, Callable javaCallable, String mvelCallable) throws Exception {
    Parser parser = new Parser(new Tokenizer(script).tokenize());
    Unit unit = parser.script();
    unit.reduceAll();

    // Compile ASM.
    Class<?> generated = new AsmCodeEmitter(unit).write(unit);
    Method asmCallable = javaCallable.lookup(generated);

    // Compile MVEL.
    String mvel = new MvelCodeEmitter(unit).write(unit);
    mvel += "; " + mvelCallable;
    Serializable compiledMvel = MVEL.compileExpression(mvel);

    // Assert validity.
    Object javaGen = javaCallable.call(asmCallable);
    Object mvelGen = MVEL.executeExpression(compiledMvel, new HashMap());
    assertNotNull(javaGen);
    assertNotNull(mvelGen);
    assertEquals(javaGen, mvelGen);

    // Warm up JVM.
    for (int i = 0; i < 15000; i++) {
       javaCallable.call(asmCallable);
       MVEL.executeExpression(compiledMvel, new HashMap());
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < RUNS; i++) {
      MVEL.executeExpression(compiledMvel, new HashMap());
    }
    System.out.println("Mvel runtime: " + (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < RUNS; i++) {
      javaCallable.call(asmCallable);
    }
    System.out.println("Asm runtime: " + (System.currentTimeMillis() - start));
  }

  public static interface Callable {
    Method lookup(Class target) throws Exception;
    Object call(Method target) throws Exception;
  }
}
