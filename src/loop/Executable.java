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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Executable {
  private volatile String source;      // Raw source code, discarded after compile.
  private final List<String> lines;    // Loop source code lines (for error tracing).

  private Scope scope;
  private Node node;
      // If a fragment and not a whole unit (mutually exclusive with unit)

  private List<ParseError> parseErrors;
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
    } catch (LoopCompileException e) {
      // Ignored.
    }
    if (!parser.getErrors().isEmpty())
      this.parseErrors = parser.getErrors();

    return unit;
  }

  public void printParseErrorsIfNecessary() {
    if (parseErrors != null)
      printErrors(getParseErrors());
  }

  public void printErrors(List<AnnotatedError> errors) {
    for (int i = 0, errorsSize = errors.size(); i < errorsSize; i++) {
      AnnotatedError error = errors.get(i);
      System.out.println((i + 1) + ") " + error.getMessage());
      System.out.println();

      // Show some context around this file.
      int lineNumber = error.line() + 1;
      int column = error.column();

      // Unwrap to the previous line if the highlighted line is empty.
      if (lineNumber > 0 && (lines.get(lineNumber - 1).trim().isEmpty() || column == 0)) {
        lineNumber -= 1;
        column = lines.get(Math.max(0, lineNumber - 1)).length();
      }

      if (error.line() > 0)
        System.out.println("  " + error.line() + ": " + lines.get(lineNumber - 2));

      System.out.println("  " + lineNumber + ": " + lines.get(Math.max(0, lineNumber - 1)));
      int spaces = column + Integer.toString(lineNumber).length() + 1;
      System.out.println("  " + whitespace(spaces) + "^\n");
    }
  }

  public boolean runMain() {
    return runMain;
  }

  public boolean hasParseErrors() {
    return parseErrors != null;
  }

  public void compile() {
    Unit unit = parse(source);
    if (hasParseErrors())
      return;

    AsmCodeEmitter codeEmitter = new AsmCodeEmitter(unit);
    this.scope = unit;
//    this.emittedNodes = codeEmitter.getEmittedNodeMap();
    this.compiled = codeEmitter.write(unit, true);

    requireImports(unit.imports());

    this.source = null;
  }

  private void requireImports(Set<RequireDecl> imports) {
    for (RequireDecl requireDecl : imports) {
      if (requireDecl.javaLiteral != null)
        try {
          Class.forName(requireDecl.javaLiteral);
        } catch (ClassNotFoundException e) {
          if (parseErrors == null)
            parseErrors = new ArrayList<ParseError>();

          parseErrors.add(new ParseError("Unable to find Java type for import: "
              + requireDecl.javaLiteral, requireDecl.sourceLine, requireDecl.sourceColumn));
        }
    }
  }

  public void compileExpression(Scope scope) {
    this.scope = scope;
    Parser parser = new Parser(new Tokenizer(source).tokenize());
    Node line = parser.line();
    if (hasParseErrors())
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

    if (hasParseErrors())
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
  public List<AnnotatedError> getParseErrors() {
    return (List) parseErrors;
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
