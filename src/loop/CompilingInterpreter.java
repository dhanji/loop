package loop;

import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.type.TypeSolver;
import loop.type.scope.BaseScope;
import org.mvel2.MVEL;

import java.io.*;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class CompilingInterpreter {
  private final Class<?> main;

  public CompilingInterpreter(Class<?> main) {
    this.main = main;
  }

  public void run() {
    MVEL.eval("main()");
  }

  public static void execute(String file) {
    Unit unit;
    try {
      unit = load(new FileReader(new File(file)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    for (FunctionDecl fn : unit.functions()) {
      System.out.println(Parser.stringify(fn));
      TypeSolver.solve(fn, new BaseScope());
    }
    System.out.println();

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
