package loop;

import loop.ast.script.Unit;
import org.mvel2.MVEL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {
  public static void main(String[] args) {
    if (args.length == 0) {
      LoopShell.shell();
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("ARGV", Arrays.asList(args));

    if (args.length > 1)
      run(args[0], false, map);
    else
      run(args[0]);
  }

  public static Object run(String file) {
    return run(file, false);
  }

  public static Object run(String file, boolean print) {
    return run(file, print, new HashMap<String, Object>());
  }

  public static Object run(String file, boolean print, Map<String, Object> context) {
    String unit = loopCompile(file);
    if (print)
      System.out.println(unit);

    // Invoke main!
    unit += "; main();";

    return MVEL.eval(unit, context);
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
    List<String> lines = new ArrayList<String>();
    StringBuilder builder;
    try {
      BufferedReader br = new BufferedReader(reader);

      builder = new StringBuilder();
      while (br.ready()) {
        String line = br.readLine();
        builder.append(line);
        builder.append('\n');

        lines.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String input = builder.toString();
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Unit unit = null;
    try {
      unit = parser.script();
      unit.reduceAll();
    } catch (LoopSyntaxException e) {
      // Ignore as it will be rethrown.
    }

    if (!parser.getErrors().isEmpty()) {
      printErrors(lines, parser);

      throw new LoopSyntaxException("Syntax errors exist", parser.getErrors());
    }
    return unit;
  }

  private static void printErrors(List<String> lines, Parser parser) {
    List<ParseError> errors = parser.getErrors();
    for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
      ParseError error = errors.get(i);
      System.out.println((i + 1) + ") " + error.getMessage());
      System.out.println();

      // Show some context around this file.
      int lineNumber = error.line() + 1;
      int column = error.column();

      // Unwrap to the previous line if the highlighted line is empty.
      if (lineNumber > 0 && (lines.get(lineNumber - 1).trim().isEmpty() || column == 0)) {
        lineNumber-= 1;
        column = lines.get(lineNumber - 1).length();
      }

      if (error.line() > 0)
        System.out.println("  " + error.line() + ": " + lines.get(lineNumber - 2));

      System.out.println("  " + lineNumber + ": " + lines.get(lineNumber - 1));
      int spaces = column + Integer.toString(lineNumber).length() + 1;
      System.out.println("  " + whitespace(spaces) + "^\n");
    }
  }

  private static String whitespace(int amount) {
    StringBuilder builder = new StringBuilder(amount);
    for (int i = 0; i < amount; i++) {
      builder.append(' ');
    }
    return builder.toString();
  }

  public static void error(String error) {
    throw new LoopExecutionException(error);
  }
}
