package loop;

import loop.ast.ClassDecl;
import loop.ast.Node;
import loop.ast.script.FunctionDecl;
import loop.ast.script.RequireDecl;
import loop.ast.script.Unit;
import loop.runtime.Scope;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reprents an executable loop program (or script). An executable is produced
 * from loop source code by pushing it through the following phases that represent
 * "compilation".
 *
 * The output of each phase is used as input to the next phase.
 *
 * <ol>
 *   <li>Tokenizer (Tokenizing) - converts the text of the program to well-understood tokens (sometimes called lexing)</li>
 *   <li>Tokenizer (Normalizing) - inserts additional tokens as appropriate to convert context-sensitive
 *      grammatical constructs to relatively regular constructs</li>
 *   <li>Parser - processes the stream of tokens to create productions in the form of an AST</li>
 *   <li>Reducer - strips the AST of redundant or crufty nodes to make a compact AST</li>
 *   <li>Verifier - Analyzes the compact AST for scope, symbol and import errors and reports them</li>
 *   <li>AsmCodeEmitter - Translates the compact AST into JVM bytecode (loadable Classes)</li>
 *   <li>LoopClassLoader - Loads the raw bytecode into a special classloader during execution</li>
 * </ol>
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Executable {
  private static final Pattern INDENT_REGEX = Pattern.compile("^(\\s+)");
  private static final int MAX_BACKTRACK_LINES = 5;

  private volatile String source;      // Raw source code, discarded after compile.
  private final List<String> lines;    // Loop source code lines (for error tracing).

  private Scope scope;

  private List<AnnotatedError> staticErrors;
  private Class<?> compiled;
  private boolean runMain;
  private final String file;

  public Executable(Reader source) {
    this(source, null);
  }

  public Executable(Reader source, String file) {
    this.file = file;

    List<String> lines = new ArrayList<String>();
    StringBuilder builder;
    try {
      BufferedReader br = new BufferedReader(source);

      builder = new StringBuilder();
      while (br.ready()) {
        String line = br.readLine();
        if (line == null)
          break;

        builder.append(line);
        builder.append('\n');

        lines.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.source = builder.toString();
    this.lines = lines;
  }

  private Unit parse(String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Unit unit = null;
    try {
      unit = parser.script(file);
      unit.reduceAll();

      this.scope = unit;
    } catch (RuntimeException e) {
      // Ignored.
      System.out.println("Parse errors exist.");
      if (!(e instanceof LoopCompileException))
        e.printStackTrace();
    }

    if (!parser.getErrors().isEmpty())
      this.staticErrors = parser.getErrors();

    return unit;
  }

  public boolean verify(Unit unit) {
    return null == (this.staticErrors = new Verifier(unit).verify());
  }

  public String printStaticErrorsIfNecessary() {
    if (staticErrors != null)
      return printErrors(getStaticErrors());

    return "";
  }

  public void printErrorsTo(PrintStream out, List<AnnotatedError> errors) {
    for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
      AnnotatedError error = errors.get(i);
      out.println((i + 1) + ") " + error.getMessage());
      out.println();

      // Unwrap to previous line if column is 0, or line is empty.
      int errorLineNumber = error.line(), column = error.column();
      if (errorLineNumber >= lines.size())
        errorLineNumber = lines.size() - 1;

      if (error.column() == 0 || lines.get(errorLineNumber).trim().isEmpty()) {
        errorLineNumber = Math.max(0, errorLineNumber - 1);
        column = lines.get(errorLineNumber).length();
      }

      String thisLine = lines.get(errorLineNumber);

      // Detect the nearest indent-drop above the error, but only if there is an indent.
      int indent, startLine = errorLineNumber;

      Matcher matcher = INDENT_REGEX.matcher(thisLine);
      if (matcher.find()) {
        indent = matcher.group(1).length();

        // Find an indent-drop before this line.
        ListIterator<String> lineIterator = lines.listIterator(errorLineNumber);
        int backtrackCount = 0;
        while (backtrackCount <= MAX_BACKTRACK_LINES && lineIterator.hasPrevious()) {
          String previous = lineIterator.previous();
          Matcher previousMatcher = INDENT_REGEX.matcher(previous);
          if (!previousMatcher.find() || previousMatcher.group(1).length() < indent) {
            startLine = Math.max(0, errorLineNumber - backtrackCount - 1);
            break;
          }

          backtrackCount++;
        }

        // If we weren't able to find an indent drop within MAX_BACKTRACK_LINES,
        // just show MAX_BACKTRACK_LINES of context.
        if (startLine == errorLineNumber) {
          startLine -= backtrackCount;
        }

      } // otherwise this is an unindented line anyway.

      // Print from startLine to error line.
      for (int lineNumber = startLine; lineNumber <= errorLineNumber; lineNumber++) {
        String line = lines.get(lineNumber);
        int lineNumberLabel = lineNumber + 1;

        // if there is a change in line number label width, we need to change the indent
        // so that everything lines up properly.
        int leader = 2;
        if (Math.floor(Math.log10(lineNumberLabel)) > Math.floor(Math.log10(lineNumber)))
          leader--;

        out.println(whitespace(leader) + lineNumberLabel + ":  " + line);
      }

      // Caret line (^)
      int spaces = column + Integer.toString(errorLineNumber).length() + 1;
      out.println("  " + whitespace(spaces) + "^\n");
    }
  }

  public String printErrors(List<AnnotatedError> errors) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    printErrorsTo(new PrintStream(buffer), errors);
    String errorText = buffer.toString();

    System.out.print(errorText);
    return errorText;
  }

  public String file() {
    return file;
  }

  public boolean runMain() {
    return runMain;
  }

  public boolean hasErrors() {
    return staticErrors != null;
  }

  private void requireJavaImports(Set<RequireDecl> imports) {
    for (RequireDecl requireDecl : imports) {
      if (requireDecl.javaLiteral != null)
        try {
          Class.forName(requireDecl.javaLiteral);
        } catch (ClassNotFoundException e) {
          if (staticErrors == null)
            staticErrors = new ArrayList<AnnotatedError>();

          staticErrors.add(new StaticError("Unable to find Java type for import: "
              + requireDecl.javaLiteral, requireDecl.sourceLine, requireDecl.sourceColumn));
        }
    }
  }

  public Object main(String[] commandLine) {
    FunctionDecl main = scope.resolveFunction("main", false);
    if (main != null) {
      int args = main.arguments().children().size();
      if(commandLine == null)
    	  commandLine = new String[] {};
      try {
        if (args == 0)
          return compiled.getDeclaredMethod("main").invoke(null);
        else
          return compiled.getDeclaredMethod("main", Object.class).invoke(null, Arrays.asList(commandLine));
      } catch (NoSuchMethodException e) {
        System.out.println("Incorrect main method declaration in: " + file);
      } catch (InvocationTargetException e) {
        // Unwrap Java stack trace using our special wrapper exception.
        Throwable cause = e.getCause();
        StackTraceSanitizer.clean(cause);

        if (cause instanceof VerifyError)
          throw (Error) cause;

        // Rethrow cleaned up exception.
        if (cause instanceof RuntimeException)
          throw (RuntimeException) cause;

        throw new RuntimeException(cause);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    } else {
      // Attempt to force class initialization.
      try {
        Class.forName(compiled.getName(), true, LoopClassLoader.CLASS_LOADER);
      } catch (ClassNotFoundException e) {
        throw new Error("Not supposed to happen. A previously loaded class disappeared.", e);
      }
    }
    return null;
  }

  public void compile() {
    Unit unit = parse(source);
    if (hasErrors())
      return;

    // Recursively loads and compiles all dependency modules.
    List<AnnotatedError> depErrors = unit.loadDeps(file);
    if (depErrors != null) {
      this.staticErrors = depErrors;
      return;
    }

    // Run the verifier just before we emit code.
    if (!verify(unit))
      return;

    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(unit);
    this.scope = unit;
    this.compiled = codeEmitter.write(unit);

    requireJavaImports(unit.imports());

    this.source = null;
  }

  public void compileExpression(Unit scope) {
    this.scope = scope;

    if (!verify(scope))
      return;

    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(scope);
    this.compiled = codeEmitter.write(scope);
    this.source = null;

    requireJavaImports(scope.requires());
  }

  public void compileClassOrFunction(Unit scope) {
    this.scope = scope;
    List<Token> tokens = new Tokenizer(source).tokenize();
    Parser parser = new Parser(tokens);
    FunctionDecl functionDecl = parser.functionDecl();
    ClassDecl classDecl = null;
    Node node;
    if (null == functionDecl) {
      classDecl = parser.classDecl();
      node = classDecl;
    } else
      node = functionDecl;

    if (hasErrors())
      return;

    if (node == null) {
      this.staticErrors = Arrays.<AnnotatedError>asList(
          new StaticError("malformed function definition",
          tokens.get(tokens.size() - 1)));
      return;
    }

    new Reducer(node).reduce();

    if (!verify(scope))
      return;

    // We don't need to actually compile this code, yet.

    this.source = null;
    requireJavaImports(scope.requires());

    if (functionDecl != null)
      scope.declare(functionDecl);
    else
      scope.declare(classDecl);
  }

  private static String whitespace(int amount) {
    StringBuilder builder = new StringBuilder(amount);
    for (int i = 0; i < amount; i++) {
      builder.append(' ');
    }
    return builder.toString();
  }

  public Class<?> getCompiled() {
    return compiled;
  }

  public void runMain(boolean runMain) {
    if (runMain)
      this.runMain = runMain;
  }

  @SuppressWarnings("unchecked")
  public List<AnnotatedError> getStaticErrors() {
    return (List) staticErrors;
  }

  public Scope getScope() {
    return scope;
  }
}
