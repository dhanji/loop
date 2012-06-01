package loop;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import loop.ast.Assignment;
import loop.ast.Node;
import loop.ast.Variable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopShell {
  private static Map<String, Object> shellContext;

  public static void shell() {
    System.out.println("loOp (http://looplang.org)");
    System.out.println("     by Dhanji R. Prasanna\n");

    try {
      ConsoleReader reader = new ConsoleReader();
      reader.addCompleter(new MetaCommandCompleter());

      Unit shellScope = new Unit(null, ModuleDecl.SHELL);
      FunctionDecl main = new FunctionDecl("main", null);
      shellScope.declare(main);
      shellContext = new HashMap<String, Object>();

      boolean inFunction = false;

      // Used to build up multiline statement blocks (like functions)
      StringBuilder block = null;
      //noinspection InfiniteLoopStatement
      do {
        String prompt = (block != null) ? "|    " : ">> ";
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
          shellScope.loadDeps("<shell>");
          continue;
        }

        if (line.startsWith(":q") || line.startsWith(":quit")) {
          quit();
        }

        if (line.startsWith(":h") || line.startsWith(":help")) {
          printHelp();
        }

        if (line.startsWith(":run")) {
          String[] split = line.split("[ ]+", 2);
          if (split.length < 2 || !split[1].endsWith(".loop"))
            System.out.println("You must specify a .loop file to run.");
          Loop.run(split[1]);
          continue;
        }

        if (line.startsWith(":r") || line.startsWith(":reset")) {
          System.out.println("Context reset.");
          shellScope = new Unit(null, ModuleDecl.SHELL);
          main = new FunctionDecl("main", null);
          shellScope.declare(main);
          shellContext = new HashMap<String, Object>();
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
        } else if (isDangling(line)) {
          if (block == null)
            block = new StringBuilder();

          block.append(line).append('\n');
          continue;
        }

        if (block != null) {
          rawLine = block.append(line).toString();
          block = null;
        }

        // First determine what kind of expression this is.
        main.children().clear();

        // OK execute expression.
        try {
          printResult(evalInFunction(rawLine, main, shellScope, true));
        } catch (ClassCastException e) {
          StackTraceSanitizer.cleanForShell(e);
          System.out.println("#error: " + e.getMessage());
          System.out.println();
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

  private static void printHelp() {
    System.out.println("loOp Shell v1.0");
    System.out.println("  :run <file.loop>  - executes the specified loop file");
    System.out.println("  :reset            - discards current shell context (variables, funcs, etc.)");
    System.out.println("  :imports          - lists all currently imported loop modules and Java types");
    System.out.println("  :functions        - lists all currently defined functions by signature");
    System.out.println("  :type <expr>      - prints the type of the given expression");
    System.out.println("  :javatype <expr>  - prints the underlying java type (for examining loop internals)");
    System.out.println("  :quit (or Ctrl-D) - exits the loop shell");
    System.out.println("  :help             - prints this help card");
    System.out.println();
    System.out.println("  Hint :h is short for :help, etc.");
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
    rawLine = rawLine.trim() + '\n';
    Executable executable = new Executable(new StringReader(rawLine));
    Node parsedLine;
    try {
      Parser parser = new Parser(new Tokenizer(rawLine).tokenize(), shellScope);
      parsedLine = parser.line();
      if (parsedLine == null || !parser.getErrors().isEmpty()) {
        executable.printErrors(parser.getErrors());
        return "";
      }

      // If this is an assignment, just check the rhs portion of it.
      // This is a bit hacky but prevents verification from balking about new
      // vars declared in the lhs.
      if (parsedLine instanceof Assignment) {
        Assignment assignment = (Assignment) parsedLine;
        func.children().add(assignment.rhs());
      } else
        func.children().add(parsedLine);

      // Compress nodes and eliminate redundancies.
      new Reducer(func).reduce();

      shellScope.loadDeps("<shell>");
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
      Object result = Loop.safeEval(executable, null);

      if (addToWhereBlock && parsedLine instanceof Assignment) {
        Assignment assignment = (Assignment) parsedLine;
        if (assignment.lhs() instanceof Variable) {
          String name = ((Variable) assignment.lhs()).name;
          shellContext.put(name, result);

          // Loop up the value of the RHS of the variable from the shell context,
          // if this is the second reference to the same variable.
          assignment.setRhs(new Parser(new Tokenizer("`loop.LoopShell`.shellObtain(" + name + ")").tokenize()).parse());
        }
        func.declareLocally(parsedLine);
      }

      return result;
    } finally {
      ModuleLoader.reset();     // Cleans up the loaded classes.
    }
  }

  private static void printResult(Object result) {
    if (result instanceof Closure) {
      Closure fun = (Closure) result;
      System.out.println("#function: " + fun.name);
    } else if (result instanceof Set) {
      String r = result.toString();
      System.out.println('{' + r.substring(1, r.length() - 1) + '}');
    }
    else
      System.out.println(result == null ? "#nothing" : result);
  }

  private static boolean isLoadCommand(String line) {
    return line.startsWith(":run");
  }

  private static void quit() {
    System.out.println("Bye.");
    System.exit(0);
  }

  // For tracking multiline expressions.
  private static int braces = 0, brackets = 0, parens = 0;

  private static boolean isDangling(String line) {
    for (Token token : new Tokenizer(line).tokenize()) {
      switch (token.kind) {
        case LBRACE:
          braces++;
          break;
        case LBRACKET:
          brackets++;
          break;
        case LPAREN:
          parens++;
          break;
        case RBRACE:
          braces--;
          break;
        case RBRACKET:
          brackets--;
          break;
        case RPAREN:
          parens--;
          break;
      }
    }

    return braces > 0 || brackets > 0 || parens > 0;
  }

  private static class MetaCommandCompleter implements Completer {
    private final List<String> commands = Arrays.asList(
        ":help",
        ":run",
        ":quit",
        ":reset",
        ":type",
        ":imports",
        ":javatype",
        ":functions"
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
