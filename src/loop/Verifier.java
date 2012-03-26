package loop;

import loop.ast.Assignment;
import loop.ast.Call;
import loop.ast.ClassDecl;
import loop.ast.ConstructorCall;
import loop.ast.DestructuringPair;
import loop.ast.Guard;
import loop.ast.MapPattern;
import loop.ast.Node;
import loop.ast.PatternRule;
import loop.ast.RegexLiteral;
import loop.ast.Variable;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.runtime.regex.NamedPattern;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

/**
 * Walks the reduced AST looking for scope, symbol and module/import errors. This phase is intended
 * to catch all obvious errors that a static analysis can reveal.
 * <p/>
 * Nothing in this phase affects the semantics of the program. This phase can be completely skipped
 * with no ill-effects for semantically correct programs.
 */
public class Verifier {
  private static final Set<String> GLOBALS = new HashSet<String>(Arrays.asList(
      "ARGV", "ENV"
  ));
  private final Unit unit;
  private final Stack<FunctionContext> functionStack = new Stack<FunctionContext>();

  private List<StaticError> errors;

  public Verifier(Unit unit) {
    this.unit = unit;
  }

  public List<StaticError> verify() {
    for (FunctionDecl functionDecl : unit.functions()) {
      verify(functionDecl);
    }

    return errors;
  }

  private void verify(FunctionDecl functionDecl) {
    functionStack.push(new FunctionContext(functionDecl));

    // some basic function signature verification.
    if (functionDecl.patternMatching && functionDecl.arguments().children().isEmpty()) {
      addError("Cannot have zero arguments in a pattern matching function" +
          " (did you mean to use '->')", functionDecl.sourceLine, functionDecl.sourceColumn);
    } else
      verifyNodes(functionDecl.children());

    for (Node inner : functionDecl.whereBlock) {
      if (inner instanceof FunctionDecl)
        verify((FunctionDecl) inner);
      else
        verifyNode(inner);
    }

    functionStack.pop();
  }

  private void verifyNodes(List<Node> nodes) {
    for (Node child : nodes) {

      if (child instanceof PatternRule) {
        Stack<PatternRule> patternRuleStack = functionStack.peek().patternRuleStack;
        patternRuleStack.push((PatternRule) child);
        verifyNode(child);
        patternRuleStack.pop();

      } else if (child instanceof FunctionDecl) // Closures in function body.
        verify((FunctionDecl)child);
      else
        verifyNode(child);
    }
  }

  private void verifyNode(Node node) {
    if (node == null)
      return;

    // Pre-order traversal.
    verifyNodes(node.children());

    if (node instanceof Call) {
      Call call = (Call) node;

      // Skip resolving property derefs.
      if (!call.isFunction || call.isJavaStatic() || call.isPostfix())
        return;

      verifyNode(call.args());

      FunctionDecl targetFunction = resolveCall(call.name);
      if (targetFunction == null)
        addError("Cannot resolve function: " + call.name, call.sourceLine, call.sourceColumn);
      else {
        // Check that the args are correct.
        int targetArgs = targetFunction.arguments().children().size();
        int calledArgs = call.args().children().size();
        if (calledArgs != targetArgs)
          addError("Incorrect number of arguments to: " + targetFunction.name()
              + " (expected " + targetArgs + ", found "
              + calledArgs + ")",
              call.sourceLine, call.sourceColumn);
      }

    } else if (node instanceof PatternRule) {
      PatternRule patternRule = (PatternRule) node;
      verifyNode(patternRule.rhs);

      // Some sanity checking of pattern rules.
      FunctionDecl function = functionStack.peek().function;
      int argsSize = function.arguments().children().size();
      int patternsSize = patternRule.patterns.size();
      if (patternsSize != argsSize)
        addError("Incorrect number of patterns in: '" + function.name() + "' (expected " + argsSize
            + " found " + patternsSize + ")", patternRule.sourceLine, patternRule.sourceColumn);

    } else if (node instanceof Guard) {
      Guard guard = (Guard) node;
      verifyNode(guard.expression);
      verifyNode(guard.line);
    } else if (node instanceof Variable) {
      Variable var = (Variable) node;
      if (!resolveVar(var.name))
        addError("Cannot resolve symbol: " + var.name, var.sourceLine, var.sourceColumn);
    } else if (node instanceof ConstructorCall) {
      ConstructorCall call = (ConstructorCall) node;
      if (!resolveType(call))
        addError("Cannot resolve type (either as loop or Java): "
            + (call.modulePart == null ? "" : call.modulePart) + call.name,
            call.sourceLine, call.sourceColumn);
    }
  }

  private boolean resolveType(ConstructorCall call) {
    ClassDecl classDecl = unit.resolve(call.name);
    if (classDecl != null) {
      return true;
    }

    String javaType;
    if (call.modulePart != null)
      javaType = call.modulePart + call.name;   // if it's an FQN
    else
      javaType = unit.resolveJavaType(call.name);  // resolve via require clause

    if (javaType == null)
      return false;

    // Attempt to resolve as a Java type.
    try {
      Class<?> clazz = Class.forName(javaType);

      int size = call.args().children().size();
      for (Constructor<?> constructor : clazz.getConstructors()) {
        if (constructor.getParameterTypes().length == size)
          return true;
      }
    } catch (ClassNotFoundException e) {
      return false;
    }

    return false;
  }

  private boolean resolveVar(String name) {
    ListIterator<FunctionContext> iterator = functionStack.listIterator(functionStack.size());
    FunctionContext thisFunction = functionStack.peek();

    // First of all, attempt to resolve as a patterm match.
    if (thisFunction.function.patternMatching && !thisFunction.patternRuleStack.empty()) {
      PatternRule patternRule = thisFunction.patternRuleStack.peek();
      for (Node pattern : patternRule.patterns) {
        if (pattern instanceof MapPattern) {
          // Look in destructuring pairs
          for (Node child : pattern.children()) {
            Variable lhs = (Variable)((DestructuringPair)child).lhs;
            if (name.equals(lhs.name))
              return true;
          }

        } else if (pattern instanceof RegexLiteral) {
          RegexLiteral regexLiteral = (RegexLiteral) pattern;
          try {
            NamedPattern compiled = NamedPattern.compile(regexLiteral.value);
            if (compiled.groupNames().contains(name))
              return true;

          } catch (RuntimeException e) {
            addError("Malformed regular expression: " + regexLiteral.value
                + " (" + e.getMessage() + ")",
                regexLiteral.sourceLine, regexLiteral.sourceColumn);
          }
        } else {
          for (Node node : pattern.children()) {
            if (node instanceof Variable && name.equals(((Variable) node).name))
              return true;
          }
        }
      }
    }

    // Attempt to resolve in args.
    for (Node node : thisFunction.function.arguments().children()) {
      ArgDeclList.Argument argument = (ArgDeclList.Argument) node;
      if (name.equals(argument.name()))
        return true;
    }

    // Keep searching up the stack until we resolve this symbol or die trying!.
    while (iterator.hasPrevious()) {
      FunctionDecl functionDecl = iterator.previous().function;
      List<Node> whereBlock = functionDecl.whereBlock;

      // Then attempt to resolve in local function scope.
      for (Node node : whereBlock) {
        if (node instanceof Assignment) {
          Assignment assignment = (Assignment) node;
          if (name.equals(((Variable) assignment.lhs()).name))
            return true;
        }
      }
    }

    // Finally this could be a function pointer.
    return resolveCall(name) != null;
  }

  private FunctionDecl resolveCall(String name) {
    ListIterator<FunctionContext> iterator = functionStack.listIterator(functionStack.size());
    while (iterator.hasPrevious()) {
      FunctionDecl functionDecl = iterator.previous().function;
      List<Node> whereBlock = functionDecl.whereBlock;

      // Well, first see if this is a direct call (usually catches recursion).
      if (name.equals(functionDecl.name()))
        return functionDecl;

      // First attempt to resolve in local function scope.
      for (Node node : whereBlock) {
        if (node instanceof FunctionDecl) {
          FunctionDecl inner = (FunctionDecl) node;
          if (name.equals(inner.name()))
            return inner;
        }
      }
    }


    // Then attempt to resolve in module.
    FunctionDecl target = unit.get(name);
    if (target != null)
      return target;

    // Finally, attempt to resolve in imported modules.
    // ...

    return null;
  }

  private void addError(String message, int line, int column) {
    if (errors == null)
      errors = new ArrayList<StaticError>();

    errors.add(new StaticError(message, line, column));
  }

  public static class FunctionContext {
    private final FunctionDecl function;
    private Stack<PatternRule> patternRuleStack;

    public FunctionContext(FunctionDecl function) {
      this.function = function;
      if (function.patternMatching)
        this.patternRuleStack = new Stack<PatternRule>();
    }
  }
}
