package loop;

import loop.MvelCodeEmitter.SourceLocation;
import loop.ast.ClassDecl;
import loop.ast.Node;
import loop.ast.script.FunctionDecl;
import loop.ast.script.RequireDecl;
import loop.ast.script.Unit;
import loop.runtime.Scope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Executable {
  private static final Pattern INDENT_REGEX = Pattern.compile("^(\\s+)");
  private static final int MAX_BACKTRACK_LINES = 5;

  private volatile String source;      // Raw source code, discarded after compile.
  private final List<String> lines;    // Loop source code lines (for error tracing).

  private Scope scope;
  private Node node;
      // If a fragment and not a whole unit (mutually exclusive with unit)

  private List<StaticError> staticErrors;
  private TreeMap<SourceLocation, Node> emittedNodes;
  private Class<?> compiled;
  private boolean runMain;

  public Executable(Reader source) {
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
      unit = parser.script();
      unit.reduceAll();
    } catch (RuntimeException e) {
      // Ignored.
      System.out.println("Parse errors exist.");
    }

    if (!parser.getErrors().isEmpty())
      this.staticErrors = parser.getErrors();
    else
      this.staticErrors = new Verifier(unit).verify();

    return unit;
  }

  public void printStaticErrorsIfNecessary() {
    if (staticErrors != null)
      printErrors(getStaticErrors());
  }

  public void printErrors(List<AnnotatedError> errors) {
    for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
      AnnotatedError error = errors.get(i);
      System.out.println((i + 1) + ") " + error.getMessage());
      System.out.println();

      // Unwrap to previous line if column is 0, or line is empty.
      int errorLineNumber = error.line(), column = error.column();
      if (error.column() == 0 || lines.get(error.line()).trim().isEmpty()) {
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

        System.out.println(whitespace(leader) + lineNumberLabel + ":  " + line);
      }

      // Caret line (^)
      int spaces = column + Integer.toString(errorLineNumber).length() + 1;
      System.out.println("  " + whitespace(spaces) + "^\n");
    }
  }

  public void _printErrors(List<AnnotatedError> errors) {
    for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
      AnnotatedError error = errors.get(i);
      System.out.println((i + 1) + ") " + error.getMessage());
      System.out.println();

      // Show some context around this line.
      int lineNumber = error.line() + 1;
      int column = error.column();

      // Unwrap to the previous line if the highlighted line is empty.
      if (lineNumber > 0 && (lines.get(lineNumber - 1).trim().isEmpty() || column == 0)) {
        lineNumber -= 1;
        column = lines.get(Math.max(0, lineNumber - 1)).length();
      }

      // Line #1
      if (error.line() > 0) {
        String lineOne = lines.get(lineNumber - 2);
        // Print an extra line if this line is empty and the previous line is not.
        if (lineOne.trim().isEmpty() && lineNumber - 3 >= 0) {
          String lineZero = lines.get(lineNumber - 3);
          System.out.println("  " + (error.line() - 1) + ": " + lineZero);
        }
        System.out.println("  " + error.line() + ": " + lineOne);
      }

      // Line #2
      System.out.println("  " + lineNumber + ": " + lines.get(Math.max(0, lineNumber - 1)));

      // Caret line (^)
      int spaces = column + Integer.toString(lineNumber).length() + 1;
      System.out.println("  " + whitespace(spaces) + "^\n");
    }
  }

  public boolean runMain() {
    return runMain;
  }

  public boolean hasErrors() {
    return staticErrors != null;
  }

  public void compile() {
    Unit unit = parse(source);
    if (hasErrors())
      return;

    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(unit);
    this.scope = unit;
//    this.emittedNodes = codeEmitter.getEmittedNodeMap();
    this.compiled = codeEmitter.write(unit, false);

    requireImports(unit.imports());

    this.source = null;
  }

  private void requireImports(Set<RequireDecl> imports) {
    for (RequireDecl requireDecl : imports) {
      if (requireDecl.javaLiteral != null)
        try {
          Class.forName(requireDecl.javaLiteral);
        } catch (ClassNotFoundException e) {
          if (staticErrors == null)
            staticErrors = new ArrayList<StaticError>();

          staticErrors.add(new StaticError("Unable to find Java type for import: "
              + requireDecl.javaLiteral, requireDecl.sourceLine, requireDecl.sourceColumn));
        }
    }
  }

  public void compileExpression(Scope scope) {
    this.scope = scope;
    Parser parser = new Parser(new Tokenizer(source).tokenize());
    Node line = parser.line();
    if (hasErrors())
      return;

    this.node = new Reducer(line).reduce();

    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(scope);
//    this.emittedNodes = codeEmitter.getEmittedNodeMap();
    this.compiled = codeEmitter.write(node);
    this.source = null;

    requireImports(scope.requires());
  }

  public void compileClassOrFunction(Scope scope) {
    this.scope = scope;
    Parser parser = new Parser(new Tokenizer(source).tokenize());
    FunctionDecl functionDecl = parser.functionDecl();
    ClassDecl classDecl = null;
    if (null == functionDecl) {
      classDecl = parser.classDecl();
      this.node = classDecl;
    } else
      this.node = functionDecl;

    if (hasErrors())
      return;

    this.node = new Reducer(node).reduce();
    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(scope);
//    this.emittedNodes = codeEmitter.getEmittedNodeMap();
    this.compiled = codeEmitter.write(node);
    this.source = null;

    requireImports(scope.requires());

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

  public void printSourceFragment(final String message, int line, int column) {
    SourceLocation start = new SourceLocation(line - 1, column);
    SourceLocation end = new SourceLocation(line, 0);
    SortedMap<SourceLocation, Node> range = emittedNodes.subMap(start, end);
    if (range.isEmpty())
      return;

    final Node errorLocation = range.values().iterator().next();
    printErrors(Arrays.<AnnotatedError>asList(new AnnotatedError() {
      @Override
      public String getMessage() {
        return message;
      }

      @Override
      public int line() {
        return errorLocation.sourceLine;
      }

      @Override
      public int column() {
        return errorLocation.sourceColumn;
      }
    }));
  }
}
