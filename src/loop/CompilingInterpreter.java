package loop;

import loop.ast.script.Unit;
import org.mvel2.MVEL;

import java.io.*;
import java.util.HashMap;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class CompilingInterpreter {
  private final Class<?> main;

  public CompilingInterpreter(Class<?> main) {
    this.main = main;
  }

  public static void execute(String file) {
    String unit = loopCompile(file);
    System.out.println(unit);

    // Invoke main!
    unit += "; main();";

    MVEL.eval(unit, new HashMap<String, Object>());
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
}
