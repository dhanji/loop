package loop;

import loop.runtime.Tracer;
import org.mvel2.MVEL;
import org.mvel2.PropertyAccessException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {
  // Global config options for the runtime.
  static volatile boolean enableStackTraces = true;

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
    return run(file, print, context, true);
  }

  public static Object run(String file,
                           boolean print,
                           Map<String, Object> context,
                           boolean runMain) {
    Executable unit = loopCompile(file);
    if (print)
      System.out.println(unit.getCompiled());

    unit.runMain(runMain);
    return safeEval(unit, context);
  }

  public static Object eval(String expression, Map<String, Object> context) {
    Executable executable = new Executable(new StringReader(expression + '\n'));
    executable.compileExpression();

    return safeEval(executable, context);
  }

  public static Object evalFunction(String function, Map<String, Object> context) {
    Executable executable = new Executable(new StringReader(function));
    executable.compileFunction();

    return safeEval(executable, context);
  }

  private static Object safeEval(Executable executable, Map<String, Object> context) {
    try {
      return MVEL.eval(executable.getCompiled(), context);
    } catch (PropertyAccessException e) {
      String message = e.getMessage();

      // Show the source code fragment where this error occurred.
      executable.printSourceFragment(e.getLineNumber(), e.getColumn());

      if (message.contains("unresolvable property")) {
        System.out.println("I don't know that identifier =(");
      } else if (message.contains("unable to resolve method"))
        System.out.println("I don't know that method =(");
      else if (message.contains("null pointer exception")) {
        System.out.println("Oh noes, you can't dereference a null value returned from Java code!");
      } else {
        if (enableStackTraces) {
          Tracer.printCurrentTrace(executable, e, System.out);
          Tracer.complete();
        } else
          e.printStackTrace();
      }
      return new LoopError();
    } catch (LoopSyntaxException e) {
      throw e;
    } catch (Exception e) {
      if (enableStackTraces) {
        System.out.println(Tracer.getStackTrace());
        Tracer.complete();
      }
      return new LoopError();
    }
  }

  /**
   * Compiles the specified file into a binary MVEL executable.
   */
  public static Serializable compile(String file) {
    Executable unit = loopCompile(file);

    return MVEL.compileExpression(unit.getCompiled(), new HashMap<String, Object>());
  }

  private static Executable loopCompile(String file) {
    Executable executable;
    try {
      executable = new Executable(new FileReader(new File(file)));
      executable.compile();
      if (executable.hasErrors()) {
        executable.printErrors();

        throw new LoopSyntaxException("Syntax errors exist", executable);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return executable;
  }

  public static void error(String error) {
    throw new LoopExecutionException(error);
  }
}
