package loop;

import loop.ast.script.Unit;
import loop.runtime.LoopInvocationHandler;

import java.io.*;
import java.lang.reflect.Proxy;

/**
 * Converts parsed, type-solved, emitted code to Java classes.
 */
public class Loop {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      LoopShell.shell();
    }

    try {
      if (args.length > 1)
        run(args[0], args);
      else
        run(args[0]);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof FileNotFoundException) {
        System.out.println("No such file: " + e.getCause().getMessage());
        System.out.println();
        System.exit(1);
      } else
        throw e;
    }
  }

  /**
   * Executes a loop program stored in {@code file} and returns the
   * result of evaluating main()--which is optional.
   */
  public static Object run(String file) {
    Executable unit = loopCompile(file);
    unit.runMain(true);

    return safeEval(unit, null);
  }

  /**
   * Executes a loop program stored in {@code file} and returns the
   * result of evaluating main()--which is optional. Arguments can
   * be passed in which are sent to main() representing the command line.
   */
  public static Object run(String file, String[] args) {
    Executable unit = loopCompile(file);
    unit.runMain(true);

    return safeEval(unit, args);
  }

  /**
   * Executes a loop program read from {@code reader }and returns the
   * result of evaluating main()--which is optional. Arguments can
   * be passed in which are sent to main() representing the command line.
   */
  public static Object run(String name, Reader reader, String[] args) {
    Executable unit = loopCompile(name, reader);
    unit.runMain(true);

    return safeEval(unit, args);
  }

  public static Object evalClassOrFunction(String function,
                                           Unit shellScope) {
    Executable executable = new Executable(new StringReader(function));
    try {
      executable.compileClassOrFunction(shellScope);
    } catch (Exception e) {
      e.printStackTrace();
      return new LoopError("malformed function");
    }

    if (executable.hasErrors()) {
      executable.printStaticErrorsIfNecessary();
      return "";
    }

    return "ok";
  }

  static Object safeEval(Executable executable, String[] args) {
    if (executable.runMain()) {
      return executable.main(args);
    } else {
      executable.getCompiled();   // Forces class to be loaded & initialized.
      return null;
    }
  }

  /**
   * Compiles the specified file into a binary Java executable.
   */
  public static Class<?> compile(String file) {
    return loopCompile(file).getCompiled();
  }

  /**
   * Compiles the given script (fed by reader) into a binary Java executable.
   */
  public static Class<?> compile(String name, Reader reader) {
    return loopCompile(name, reader).getCompiled();
  }

  /**
   * Returns an executable that represents the compiled form of the Loop program.
   * <p/>
   * See {@link Executable} for more details on the compilation process.
   */
  private static Executable loopCompile(String file) {
    File script = new File(file);

    try {
      return loopCompile(script.getName(), new FileReader(script));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static Executable loopCompile(String name, Reader reader) {
    Executable executable = new Executable(reader, name);
    executable.compile();
    if (executable.hasErrors()) {
      String errors = executable.printStaticErrorsIfNecessary();

      throw new LoopCompileException("Syntax errors exist:\n" + errors, executable);
    }
    return executable;
  }

  public static void error(String error) {
    throw new LoopExecutionException(error);
  }

  /**
   * Returns an implementation of the given Java interface that
   * is backed by the specified Loop module. The generated Java class is loaded into
   * the common runtime Loop class loader. See {@link LoopClassLoader} for details.
   *
   * @param iface  A Java interface that you wish to implement using Loop
   * @param module The name of a Loop module minus the '.loop' extension. This name may
   *               contain a path-prefix from the current directory.
   */
  @SuppressWarnings("unchecked")
  public static <I> I implement(Class<I> iface, String module) {
    if (!iface.isInterface()) {
      throw new RuntimeException(iface + " is not an interface ");
    }

    return (I) Proxy.newProxyInstance(LoopClassLoader.CLASS_LOADER, new Class[]{iface},
        new LoopInvocationHandler(iface, module));
  }
}
