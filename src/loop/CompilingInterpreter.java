package loop;

import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.type.TypeSolver;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class CompilingInterpreter {
  private final Class<?> main;

  public CompilingInterpreter(Class<?> main) {
    this.main = main;
  }

  public void run() {
    try {
      Object o = main.getConstructor().newInstance();

      main.getMethod("main", new Class[0]).invoke(o);

    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static void execute(String file) {
    Unit unit = null;
    try {
      unit = load(new FileReader(new File(file)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    for (FunctionDecl fn : unit.functions()) {
      System.out.println(Parser.stringify(fn));
      TypeSolver.solve(fn);
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
