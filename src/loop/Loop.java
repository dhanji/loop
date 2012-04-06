package loop;

import loop.runtime.Tracer;
import org.mvel2.PropertyAccessException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {
  // Global config options for the runtime.
  private static final Pattern UNKNOWN_MVEL_PATTERN =
      Pattern.compile("\\[Error: unresolvable property or identifier: \\??(.*)\\]");

  public static void main(String[] args) {
    if (args.length == 0) {
      LoopShell.shell();
    }

    if (args.length > 1)
      run(args[0], false);
    else
      run(args[0]);
  }

  public static Object run(String file) {
    return run(file, false, true);
  }

  public static Object run(String file, boolean print) {
    return run(file, print, true);
  }


  public static Object run(String file,
                           boolean print,
                           boolean runMain) {
    Executable unit = loopCompile(file);
    if (print)
      System.out.println(unit.getCompiled());

    unit.runMain(runMain);
    return safeEval(unit);
  }

  public static Object eval(String expression, ShellScope shellScope) {
    Executable executable = new Executable(new StringReader(expression + '\n'));
    try {
      executable.compileExpression(shellScope);
    } catch (Exception e) {
      e.printStackTrace();
      return new LoopError("malformed expression '" + expression + "'");
    }

    return safeEval(executable);
  }

  public static Object evalClassOrFunction(String function,
                                           ShellScope shellScope,
                                           Map<String, Object> context) {
    Executable executable = new Executable(new StringReader(function));
    try {
      executable.compileClassOrFunction(shellScope);
    } catch (Exception e) {
      e.printStackTrace();
      return new LoopError("malformed function");
    }

    return safeEval(executable);
  }

  private static Object safeEval(Executable executable) {
    try {
      if (executable.runMain())
        return executable.getCompiled().getDeclaredMethod("main").invoke(null);
      else
        return executable.getCompiled();
    } catch (PropertyAccessException e) {
      String message = e.getMessage();
      String loopError;
      boolean printStackTrace = false;

      if (message.contains("unresolvable property")) {
        Matcher matcher = UNKNOWN_MVEL_PATTERN.matcher(message);
        String ident = "<unknown>";
        if (matcher.find())
          ident = matcher.group(1);
        loopError = "I don't know the identifier: '" + ident + "'   =(";
      } else if (message.contains("unable to resolve method"))
        loopError = "I don't know that method =(";
      else if (message.contains("null pointer exception"))
        loopError = "Oh noes, you can't dereference a null value returned from Java code!";
      else {
        if (e.getCause() != null)
          loopError = e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage();
        else
          loopError = message;
          e.printStackTrace();
      }

      // Show the source code fragment where this error occurred.
      executable.printSourceFragment(loopError, e.getLineNumber(), e.getColumn());
      if (printStackTrace) {
        Tracer.printCurrentTrace(executable, e, System.out);
        Tracer.complete();
      }

      return (e.getCause() == null ? new LoopError(e.getMessage()) : new LoopError((Exception) e.getCause().getCause()));
    } catch (LoopCompileException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Compiles the specified file into a binary Java executable.
   */
  public static Class<?> compile(String file) {
    return loopCompile(file).getCompiled();
  }

  /**
   * Returns an executable that represents the compiled form of the Loop program.
   *
   * See {@link Executable} for more details on the compilation process.
   */
  private static Executable loopCompile(String file) {
    Executable executable;
    try {
      File script = new File(file);
      executable = new Executable(new FileReader(script), script.getName());
      executable.compile();
      if (executable.hasErrors()) {
        executable.printStaticErrorsIfNecessary();

        throw new LoopCompileException("Syntax errors exist", executable);
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
