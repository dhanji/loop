package loop;

import loop.ast.*;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.runtime.regex.NamedPattern;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * Walks the reduced AST looking for scope, symbol and module/import errors. This phase is intended
 * to catch all obvious errors that static analysis can reveal.
 * <p/>
 * Nothing in this phase affects the semantics of the program. This phase can be completely skipped
 * with no ill-effects for semantically correct programs.
 */
class Verifier {
  private final Unit unit;
  private final Stack<FunctionContext> functionStack = new Stack<FunctionContext>();

  private List<AnnotatedError> errors;

  public Verifier(Unit unit) {
    this.unit = unit;
  }

  public List<AnnotatedError> verify() {
    for (FunctionDecl functionDecl : unit.functions()) {
      verify(functionDecl);
    }

    return errors;
  }

  private void verify(FunctionDecl functionDecl) {
    functionStack.push(new FunctionContext(functionDecl));

    // Attempt to resolve the exception handler in scope.
    if (functionDecl.exceptionHandler != null) {
      verifyExceptionHandler(functionDecl);
    }

    // some basic function signature verification.
    if (functionDecl.patternMatching && functionDecl.arguments().children().isEmpty()) {
      addError("Cannot have zero arguments in a pattern matching function" +
          " (did you mean to use '->')", functionDecl.sourceLine, functionDecl.sourceColumn);
    } else
      verifyNodes(functionDecl.children());

    for (Node inner : functionDecl.whereBlock()) {
      if (inner instanceof FunctionDecl)
        verify((FunctionDecl) inner);
      else
        verifyNode(inner);
    }

    functionStack.pop();
  }

  private void verifyExceptionHandler(FunctionDecl functionDecl) {
    FunctionDecl exceptionHandler = resolveCall(functionDecl.exceptionHandler);
    if (exceptionHandler == null)
      addError("Cannot resolve exception handler: " + functionDecl.exceptionHandler,
          functionDecl.sourceLine, functionDecl.sourceColumn);
    else {
      int argsSize = exceptionHandler.arguments().children().size();
      // Verify exception handler template.
      if (!exceptionHandler.patternMatching)
        addError("Exception handler must be a pattern-matching function (did you mean '=>')",
            exceptionHandler.sourceLine, exceptionHandler.sourceColumn);
      else if (argsSize != 1) {
        addError("Exception handler must take exactly 1 argument (this one takes "
            + argsSize + ")", exceptionHandler.sourceLine, exceptionHandler.sourceColumn);
      } else {
        for (Node child : exceptionHandler.children()) {
          PatternRule rule = (PatternRule) child;

          // Should have only 1 arg pattern.
          Node patternNode = rule.patterns.get(0);
          if (patternNode instanceof PrivateField) {
            if (!RestrictedKeywords.ENSURE.equals(((PrivateField) patternNode).name()))
              addError("Illegal pattern rule in exception handler (did you mean '" +
                  RestrictedKeywords.ENSURE + "')",
                  patternNode.sourceLine, patternNode.sourceColumn);
          } else if (patternNode instanceof TypeLiteral) {
            TypeLiteral literal = (TypeLiteral) patternNode;
            if (!resolveType(literal, Throwable.class))
              addError("Cannot resolve exception type: " + literal.name, literal.sourceLine,
                  literal.sourceColumn);
          } else if (!(patternNode instanceof WildcardPattern))
            addError("Illegal pattern rule in exception handler (only Exception types allowed)",
                patternNode.sourceLine, patternNode.sourceColumn);
        }
      }
    }
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
      if (call.isJavaStatic() || call.isPostfix())
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
    } else if (node instanceof Assignment) {
      // Make sure that you cannot reassign function arguments.
      Assignment assignment = (Assignment) node;
      if (assignment.lhs() instanceof Variable) {
        Variable lhs = (Variable) assignment.lhs();

        FunctionContext functionContext = functionStack.peek();
        for (Node argument : functionContext.function.arguments().children()) {
          ArgDeclList.Argument arg = (ArgDeclList.Argument) argument;
          if (arg.name().equals(lhs.name))
            addError("Illegal argument reassignment (declare a local variable instead)",
                lhs.sourceLine, lhs.sourceColumn);
        }

//        verifyNode(assignment.rhs());
      }
    }
  }

  private boolean resolveType(TypeLiteral literal, Class<?> superType) {
    // First resolve as Loop type. Then Java type.
    ClassDecl classDecl = unit.resolve(literal.name, true);
    if (classDecl != null)
      return true;

    String javaType = unit.resolveJavaType(literal.name);
    if (javaType == null)
      return false;

    // Verify that it exists and that it is a compatible type.
    try {
      return superType.isAssignableFrom(Class.forName(javaType));
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private boolean resolveType(ConstructorCall call) {
    ClassDecl classDecl = unit.resolve(call.name, true);
    if (classDecl != null)
      return true;

    String javaType;
    if (call.modulePart != null) {
      String[] pieces = call.modulePart.split("[.]");

      // This could be an alias if there is only one namespace part to it.
      if (pieces.length == 1) {
        String alias = pieces[0];
        classDecl = unit.resolveAliasedType(alias, call.name);

        if (classDecl != null)
          return true;
      }

      javaType = call.modulePart + call.name;   // if it's an FQN
    } else
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
      List<Node> children = functionDecl.children();

      // Then attempt to resolve in function scope.
      if (resolveVarInNodes(name, children))
        return true;
      else if (resolveVarInNodes(name, functionDecl.whereBlock()))
        return true;
    }

    // Finally this could be a function pointer.
    return resolveCall(name) != null;
  }

  private boolean resolveVarInNodes(String name, List<Node> children) {
    for (Node node : children) {
      if (node instanceof Assignment) {
        Assignment assignment = (Assignment) node;
        Node lhs = assignment.lhs();
        String lhsName;
        if (lhs instanceof Variable)
          lhsName = ((Variable) lhs).name;
        else {
          // Should be a call chain, pick the first node as the local variable.
          assert lhs instanceof CallChain;
          lhsName = ((Variable)lhs.children().iterator().next()).name;
        }
        if (name.equals(lhsName))
          return true;
      }
    }
    return false;
  }

  private FunctionDecl resolveCall(String name) {
    ListIterator<FunctionContext> iterator = functionStack.listIterator(functionStack.size());
    while (iterator.hasPrevious()) {
      FunctionDecl functionDecl = iterator.previous().function;
      List<Node> whereBlock = functionDecl.whereBlock();

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

    // Then attempt to resolve in module(s).
    FunctionDecl target = unit.resolveFunction(name, true /* resolve in deps */);
    if (target != null)
      return target;

    return null;
  }

  private void addError(String message, int line, int column) {
    if (errors == null)
      errors = new ArrayList<AnnotatedError>();

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
