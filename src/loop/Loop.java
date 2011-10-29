package loop;

import loop.ast.script.Unit;
import org.mvel2.MVEL;

import java.io.*;
import java.util.HashMap;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {
  public static Object run(String file) {
    return run(file, false);
  }

  public static Object run(String file, boolean print) {
    String unit = loopCompile(file);
    if (print)
      System.out.println(unit);

    // Invoke main!
    unit += "; main();";

    return MVEL.eval(unit, new HashMap<String, Object>());
  }

  public static Serializable compile(String file) {
    String unit = loopCompile(file);

    return MVEL.compileExpression(unit, new HashMap<String, Object>());
  }

  private static String loopCompile(String file) {
    Unit unit;
    try {
      unit = load(new FileReader(new File(file)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return new CodeWriter().write(unit);
  }

  public static Unit load(Reader reader) {
    StringBuilder builder;
    try {
      BufferedReader br = new BufferedReader(reader);

      builder = new StringBuilder();
      while (br.ready()) {
        builder.append(br.readLine());
        builder.append('\n');
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Unit unit = new Parser(new Tokenizer(builder.toString()).tokenize()).script();
    unit.reduceAll();
    return unit;
  }

  public static void error(String error) {
    throw new LoopExecutionException(error);
  }
}
