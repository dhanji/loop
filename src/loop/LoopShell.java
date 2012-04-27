package loop;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import loop.ast.Assignment;
import loop.ast.Node;
import loop.ast.script.FunctionDecl;
import loop.ast.script.ModuleDecl;
import loop.ast.script.ModuleLoader;
import loop.ast.script.RequireDecl;
import loop.ast.script.Unit;
import loop.lang.LoopObject;
import loop.runtime.Closure;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

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

      Unit shellScope = new Unit(null, ModuleDecl.SHELL);
      FunctionDecl main = new FunctionDecl("main", null);
      shellScope.declare(main);

      boolean inFunction = false;

      // Used to build up multiline statement blocks (like functions)
      StringBuilder block = null;
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
          shellScope.declare(new Parser(new Tokenizer(line + '\n').tokenize()).require());
          continue;
        }

        if (line.startsWith(":q") || line.startsWith(":quit")) {
          quit();
        }

        if (line.startsWith(":r") || line.startsWith(":reset")) {
          System.out.println("Context reset.");
          shellScope = new Unit(null, ModuleDecl.SHELL);
          continue;
        }
        if (line.startsWith(":i") || line.startsWith(":imports")) {
          for (RequireDecl requireDecl : shellScope.imports()) {
            System.out.println(requireDecl.toSymbol());
          }
          System.out.println();
          continue;
        }
        if (line.startsWith(":f") || line.startsWith(":functions")) {
          for (FunctionDecl functionDecl : shellScope.functions()) {
            StringBuilder args = new StringBuilder();
            List<Node> children = functionDecl.arguments().children();
            for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
              Node arg = children.get(i);
              args.append(arg.toSymbol());

              if (i < childrenSize - 1)
                args.append(", ");
            }

            System.out.println(functionDecl.name()
                + ": (" + args.toString() + ")"
                + (functionDecl.patternMatching ? " #pattern-matching" : "")
            );
          }
          System.out.println();
          continue;
        }
        if (line.startsWith(":t") || line.startsWith(":type")) {
          String[] split = line.split("[ ]+", 2);
          if (split.length <= 1) {
            System.out.println("Give me an expression to determine the type for.\n");
            continue;
          }

          Object result = evalInFunction(split[1], main, shellScope, false);
          printTypeOf(result);
          continue;
        }

        if (line.startsWith(":javatype")) {
          String[] split = line.split("[ ]+", 2);
          if (split.length <= 1) {
            System.out.println("Give me an expression to determine the type for.\n");
            continue;
          }

          Object result = evalInFunction(split[1], main, shellScope, false);
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
        main.children().clear();

        // OK execute expression.
        try {
          printResult(evalInFunction(rawLine, main, shellScope, true));
        } catch (RuntimeException e) {
          StackTraceSanitizer.cleanForShell(e);
          e.printStackTrace();
          System.out.println();
        }

      } while (true);
    } catch (IOException e) {
      System.err.println("Something went wrong =(");
      System.exit(1);
    }
  }

  private static void printTypeOf(Object result) {
    if (result instanceof LoopError)
      System.out.println(result.toString());
    else if (result instanceof LoopObject)
      System.out.println(((LoopObject)result).getType());
    else if (result instanceof Closure)
      System.out.println("#function: " + ((Closure)result).name);
    else
      System.out.println(result == null ? "Nothing" : "#java: " + result.getClass().getName());
  }

  private static Object evalInFunction(String rawLine,
                                       FunctionDecl func,
                                       Unit shellScope,
                                       boolean addToWhereBlock) {
    rawLine = rawLine + '\n';
    Executable executable = new Executable(new StringReader(rawLine));
    Node parsedLine;
    try {
      Parser parser = new Parser(new Tokenizer(rawLine).tokenize());
      parsedLine = parser.line();
      if (!parser.getErrors().isEmpty()) {
        executable.printErrors(parser.getErrors());
        return "";
      }

      new Reducer(parsedLine).reduce();

      // If this is an assignment, just check the rhs portion of it.
      // This is a bit hacky but prevents verification from balking about new
      // vars declared in the lhs.
      if (parsedLine instanceof Assignment) {
        Assignment assignment = (Assignment) parsedLine;
        func.children().add(assignment.rhs());
      } else
        func.children().add(parsedLine);

      executable.runMain(true);
      executable.compileExpression(shellScope);

      if (executable.hasErrors()) {
        executable.printStaticErrorsIfNecessary();

        return "";
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new LoopError("malformed expression " + rawLine);
    }

    try {
      return Loop.safeEval(executable);
    } finally {
      if (addToWhereBlock && parsedLine instanceof Assignment)
        func.whereBlock.add(parsedLine);

      ModuleLoader.reset();     // Cleans up the loaded classes.
    }
  }

  private static void printResult(Object result) {
    if (result instanceof Closure) {
      Closure fun = (Closure) result;
      System.out.println("#function:" + fun.name);
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
