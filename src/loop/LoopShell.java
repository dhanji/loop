package loop;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import loop.ast.Assignment;
import loop.ast.Node;
import loop.ast.script.ModuleDecl;
import loop.ast.script.Unit;
import loop.lang.LoopObject;
import sun.org.mozilla.javascript.internal.Function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopShell {
  public static void shell() {
    System.out.println("loOp (http://looplang.org)");
    System.out.println("     by Dhanji R. Prasanna\n");

    try {
      ConsoleReader reader = new ConsoleReader();
      reader.addCompleter(new MetaCommandCompleter());

      Unit shellScope = new Unit(null, ModuleDecl.DEFAULT);
      boolean inFunction = false;

      // Used to build up multiline statement blocks (like functions)
      StringBuilder block = null;
      List<Node> assigments = new ArrayList<Node>();    // Holds any assignments so far.
      //noinspection InfiniteLoopStatement
      do {
        String prompt = inFunction ? "|    " : ">> ";
        String rawLine = reader.readLine(prompt);

        if (inFunction) {
          if (rawLine == null || rawLine.trim().isEmpty()) {
            inFunction = false;

            // Eval the function to verify it.
            printResult(Loop.evalClassOrFunction(block.toString(), shellScope));
            block = null;
            continue;
          }

          block.append(rawLine).append('\n');
          continue;
        }

        if (rawLine == null) {
          quit();
        }

        //noinspection ConstantConditions
        String line = rawLine.trim();
        if (line.isEmpty())
          continue;

        // Add a require import.
        if (line.startsWith("require ")) {
          shellScope.declare(new Parser(new Tokenizer(line).tokenize()).require());
          continue;
        }

        if (line.startsWith(":q") || line.startsWith(":quit")) {
          quit();
        }

        if (line.startsWith(":r") || line.startsWith(":reset")) {
          System.out.println("Context reset.");
          shellScope = new Unit(null, ModuleDecl.DEFAULT);
          continue;
        }
        if (line.startsWith(":t") || line.startsWith(":type")) {
          String[] split = line.split("[ ]+", 2);
          if (split.length <= 1)
            System.out.println("Give me an expression to determine the type for.");

          Object result = Loop.eval(split[1], shellScope);
          if (result instanceof LoopError)
            System.out.println(result.toString());
          else if (result instanceof LoopObject)
            System.out.println(((LoopObject)result).getType());
          else
            System.out.println(result == null ? "Nothing" : "#java: " + result.getClass().getName());
          continue;
        }

        if (line.startsWith(":javatype")) {
          String[] split = line.split("[ ]+", 2);
          if (split.length <= 1)
            System.out.println("Give me an expression to determine the type for.");

          Object result = Loop.eval(split[1], shellScope);
          if (result instanceof LoopError)
            System.out.println(result.toString());
          else
            System.out.println(result == null ? "null" : result.getClass().getName());
          continue;
        }

        // Function definitions can be multiline.
        if (line.endsWith("->") || line.endsWith("=>")) {
          inFunction = true;
          block = new StringBuilder(line).append('\n');
          continue;
        }

        // First determine what kind of expression this is.
        Node parsedLine = new Parser(new Tokenizer(rawLine).tokenize()).line();

        // Save for context, later.
        if (parsedLine instanceof Assignment)
          assigments.add(parsedLine);

        // OK execute expression.
        printResult(Loop.eval(rawLine, shellScope));
      } while (true);
    } catch (IOException e) {
      System.err.println("Something went wrong =(");
      System.exit(1);
    }
  }

  private static void printResult(Object result) {
    if (result instanceof Function) {
//      Function fun = (Function) result;
//      System.out.println("#function:" + fun.getName() + "()");
    } else
      System.out.println(result == null ? "Nothing" : result);
  }

  private static boolean isLoadCommand(String line) {
    return line.startsWith(":l") || line.startsWith(":load");
  }

  private static void quit() {
    System.out.println("Bye.");
    System.exit(0);
  }

  private static class MetaCommandCompleter implements Completer {
    private final List<String> commands = Arrays.asList(
        ":load",
        ":quit",
        ":reset",
        ":type",
        ":inspect"
    );

    private final FileNameCompleter fileNameCompleter = new FileNameCompleter();

    @Override public int complete(String buffer, int cursor, List<CharSequence> candidates) {
      if (buffer == null) {
        buffer = "";
      } else
        buffer = buffer.trim();

      // See if we should chain to the filename completer first.
      if (isLoadCommand(buffer)) {
        String[] split = buffer.split("[ ]+");

        // Always complete the first argument.
        if (split.length > 1)
          return fileNameCompleter.complete(split[split.length - 1], cursor, candidates);
      }

      for (String command : commands) {
        if (command.startsWith(buffer)) {
          candidates.add(command.substring(buffer.length()) + ' ');
        }
      }

      return cursor;
    }
  }
}
