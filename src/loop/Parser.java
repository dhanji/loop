package loop;

import loop.ast.*;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.ModuleDecl;
import loop.ast.script.RequireDecl;
import loop.ast.script.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Takes the tokenized form of a raw string and converts it to a CoffeeScript parse tree (an
 * optimized form of its AST).
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Parser {
  private final List<ParseError> errors = new ArrayList<ParseError>();
  private final List<Token> tokens;
  private Node last = null;
  private int i = 0;

  private static final Set<Token.Kind> RIGHT_ASSOCIATIVE = new HashSet<Token.Kind>();
  private static final Set<Token.Kind> LEFT_ASSOCIATIVE = new HashSet<Token.Kind>();

  static {
    RIGHT_ASSOCIATIVE.add(Token.Kind.PLUS);
    RIGHT_ASSOCIATIVE.add(Token.Kind.MINUS);
    RIGHT_ASSOCIATIVE.add(Token.Kind.DIVIDE);
    RIGHT_ASSOCIATIVE.add(Token.Kind.STAR);
    RIGHT_ASSOCIATIVE.add(Token.Kind.MODULUS);

    RIGHT_ASSOCIATIVE.add(Token.Kind.AND);
    RIGHT_ASSOCIATIVE.add(Token.Kind.OR);
    RIGHT_ASSOCIATIVE.add(Token.Kind.NOT);
    RIGHT_ASSOCIATIVE.add(Token.Kind.EQUALS);
    RIGHT_ASSOCIATIVE.add(Token.Kind.LEQ);
    RIGHT_ASSOCIATIVE.add(Token.Kind.GEQ);
    RIGHT_ASSOCIATIVE.add(Token.Kind.LESSER);
    RIGHT_ASSOCIATIVE.add(Token.Kind.GREATER);

    LEFT_ASSOCIATIVE.add(Token.Kind.DOT);
  }

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public List<ParseError> getErrors() {
    return errors;
  }

  public void addError(String message, Token token) {
    errors.add(new ParseError(message, token));
  }

  /**
   * if := IF computation
   * <p/>
   * assign := computation ASSIGN computation
   * <p/>
   * computation := chain (op chain)+ chain := term call*
   * <p/>
   * call := DOT IDENT (LPAREN RPAREN)?
   * <p/>
   * term := (literal | variable) literal := (regex | string | number) variable := IDENT
   * <p/>
   * <p/>
   * Examples --------
   * <p/>
   * (assign)
   * <p/>
   * x = "hi".tos().tos() x = 1
   * <p/>
   * (computation)
   * <p/>
   * 1 + 2 1 + 2 - 3 * 4 1.int + 2.y() - 3.a.b * 4
   * <p/>
   * --------------------
   * <p/>
   * parse := module | require | line
   */
  public Node parse() {
    Node parsed = require();
    if (null == parsed) {
      parsed = module();
    }
    if (null == parsed) {
      parsed = line();
    }

    return last = parsed;
  }

  /**
   * The top level parsing rule. Do not use parse() to parse entire programs, it is more for
   * one-line expressions.
   * <p/>
   * script := module? require* (functionDecl | classDecl)*
   */
  public Unit script() {
    chewEols();

    ModuleDecl module = module();
    chewEols();

    Unit unit = new Unit(module);
    RequireDecl require;
    do {
      require = require();
      chewEols();

      if (null != require) {
        unit.add(require);
      }
    } while (require != null);

    FunctionDecl function;
    do {
      function = functionDecl();
      chewEols();

      if (null != function) {
        unit.add(function);
      }
    } while (function != null);

    return unit;
  }

  private void chewEols() {
    // Chew up end-of-lines.
    //noinspection StatementWithEmptyBody
    while (match(Token.Kind.EOL) != null) ;
  }

  /*** Class parsing rules ***/

  /**
   * Named function parsing rule.
   */
  public FunctionDecl functionDecl() {
    return internalFunctionDecl(false);
  }

  private FunctionDecl anonymousFunctionDecl() {
    return internalFunctionDecl(true);
  }

  /**
   * Dual purpose parsing rule. Functions and anonymous functions.
   * <p/>
   * anonymousFunctionDecl := ANONYMOUS_TOKEN argDeclList? ARROW EOL (INDENT+ line EOL)
   * <p/>
   * functionDecl := (PRIVATE_FIELD | IDENT) argDeclList? ARROW EOL (INDENT+ line EOL)
   * <p/>
   * patternFunctionDecl := (PRIVATE_FIELD | IDENT) argDeclList? HASHROCKET EOL (INDENT+ line EOL)*
   */
  private FunctionDecl internalFunctionDecl(boolean anonymous) {
    List<Token> funcName = null;
    if (!anonymous) {
      funcName = match(Token.Kind.PRIVATE_FIELD);

      if (null == funcName)
        funcName = match(Token.Kind.IDENT);

      // Not a function
      if (null == funcName) {
        return null;
      }
    } else {
      if (match(Token.Kind.ANONYMOUS_TOKEN) == null)
        return null;
    }
    ArgDeclList arguments = argDeclList();
    String name = anonymous ? null : funcName.get(0).value;
    FunctionDecl functionDecl = new FunctionDecl(name, arguments);

    // If it doesn't have a thin or fat arrow, then it's not a function either.
    if (match(Token.Kind.ARROW, Token.Kind.LBRACE) == null) {
      // Fat arrow, pattern matcher.
      if (match(Token.Kind.HASHROCKET, Token.Kind.LBRACE) == null) {
        return null;
      } else
        return patternMatchingFunctionDecl(functionDecl);
    }

    // Optionally match eols here.
    chewEols();

    Node line;

    boolean shouldContinue;
    do {
      // Absorb indentation level.
      withIndent();

      // Only one expression is allowed in a function.
      line = line();
      if (line == null) {
        break;
      }

      chewEols();

      // Look for a where block attached to this function.
      whereBlock(functionDecl);

      // A function body must be terminated by } (this is ensured by the token-stream rewriter)
      if (!endOfInput() && match(Token.Kind.RBRACE) == null) {
        addError("Expected end of function, additional statements found", tokens.get(i));
        throw new LoopSyntaxException();
      } else
        shouldContinue = false;

      functionDecl.add(line);
    } while (shouldContinue);

    return functionDecl;
  }

  private FunctionDecl patternMatchingFunctionDecl(FunctionDecl functionDecl) {
    chewEols();
    do {
      withIndent();

      // Detect pattern first. Maps supercede lists.
      Node pattern = emptyMapPattern();
      if (null == pattern)
        pattern = emptyListPattern();

      if (null == pattern)
        pattern = listOrMapPattern();

      if (null == pattern)
        pattern = stringGroupPattern();

      if (null == pattern)
        pattern = regexLiteral();

      if (pattern == null)
        pattern = term();

      // Try "otherwise" default fall thru.
      if (pattern == null)
        if (match(Token.Kind.STAR) != null)
          pattern = new WildcardPattern();

      // Look for a where block at the end of this pattern matching decl.
      int currentToken = i;
      if (pattern == null)
        if (whereBlock(functionDecl)) {
          if (endOfInput() || match(Token.Kind.RBRACE) != null)
            break;
        }

      if (pattern == null) {
        addError("Pattern syntax error. Expected a pattern rule", tokens.get(currentToken));
        return null;
      }

      PatternRule rule = new PatternRule();
      rule.pattern = pattern;

      boolean guarded = false;
      while (match(Token.Kind.PIPE) != null) {
        guarded = true;

        Node guardExpression = computation();
        if (guardExpression == null)
          if (match(Token.Kind.ELSE) != null)
            guardExpression = new OtherwiseGuard();

        if (match(Token.Kind.ASSIGN) == null)
          addError("Expected ':' after guard expression", tokens.get(i - 1));

        Node line = line();
        chewEols();
        withIndent();

        Guard guard = new Guard(guardExpression, line);
        rule.add(guard);
      }

      if (!guarded) {
        if (match(Token.Kind.ASSIGN) == null)
          addError("Expected ':' after pattern", tokens.get(i));

        rule.rhs = line();
        chewEols();
      }

      functionDecl.add(rule);
      if (endOfInput() || match(Token.Kind.RBRACE) != null)
        break;
    } while (true);

    functionDecl.patternMatching = true;
    return functionDecl;
  }

  private boolean whereBlock(FunctionDecl functionDecl) {
    withIndent();
    boolean hasWhere = false;
    if (match(Token.Kind.WHERE) != null) {
      FunctionDecl helperFunction = null;
      Node assignment;
      do {
        chewEols();
        withIndent();

        assignment = variableAssignment();
        if (null == assignment)
          helperFunction = functionDecl();

        chewEols();

        if (null != helperFunction) {
          hasWhere = true;
          functionDecl.whereBlock.add(helperFunction);
        } else if (null != assignment) {
          hasWhere = true;
          functionDecl.whereBlock.add(assignment);
        }
      } while (helperFunction != null || assignment != null);
    }

    return hasWhere;
  }

  private Node stringGroupPattern() {
    if (match(Token.Kind.LPAREN) == null)
      return null;
    StringPattern pattern = new StringPattern();

    Node term;
    while ((term = term()) != null) {
      pattern.add(term);
      if (match(Token.Kind.ASSIGN) == null)
        break;
    }

    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' at end of string group pattern rule.", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return pattern;
  }

  private Node emptyMapPattern() {
    return match(Token.Kind.LBRACKET, Token.Kind.ASSIGN, Token.Kind.RBRACKET) !=
        null ? new MapPattern() : null;
  }

  private Node emptyListPattern() {
    return match(Token.Kind.LBRACKET, Token.Kind.RBRACKET) != null ? new ListDestructuringPattern() : null;
  }

  /**
   * listOrMapPattern := (LBRACKET term ((ASSIGN term)* | UNARROW term (COMMA term UNARROW term)*) RBRACKET)
   */
  private Node listOrMapPattern() {
    Node pattern;

    // We should allow the possibility of matching a type identifier.
    List<Token> type = match(Token.Kind.TYPE_IDENT);
    TypeLiteral typeLiteral = null;
    if (null != type) {
      typeLiteral = new TypeLiteral(type.get(0).value);
    }

    if (match(Token.Kind.LBRACKET) == null)
      return typeLiteral;

    Node term = term();
    if (term == null) {
      addError("Expected term after '[' in pattern rule", tokens.get(i));
      throw new LoopSyntaxException();
    }

    // This is a list denaturing rule.
    if (match(Token.Kind.ASSIGN) != null) {
      pattern = new ListDestructuringPattern();
      pattern.add(term);
      term = term();
      if (term == null) {
        addError("Expected term after ':' in list pattern rule", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }
      pattern.add(term);

      while (match(Token.Kind.ASSIGN) != null)
        pattern.add(term());

      if (match(Token.Kind.RBRACKET) == null) {
        addError("Expected ']' at end of list pattern rule", tokens.get(i - 1));
        return null;
      }

      return pattern;
    }

    // This is a list literal rule.
    boolean endList = match(Token.Kind.RBRACKET) != null;
    if (endList || match(Token.Kind.COMMA) != null) {
      pattern = new ListStructurePattern();
      pattern.add(term);
      if (endList)
        return pattern;

      term = term();
      if (null == term) {
        addError("Expected term after ',' in list pattern rule", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }
      pattern.add(term);

      while (match(Token.Kind.COMMA) != null) {
        term = term();
        if (null == term) {
          addError("Expected term after ',' in list pattern rule", tokens.get(i - 1));
          throw new LoopSyntaxException();
        }

        pattern.add(term);
      }

      if (match(Token.Kind.RBRACKET) == null) {
        addError("Expected ']' at end of list pattern rule", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      return pattern;
    }

    // This is a map pattern.
    pattern = new MapPattern();
    if (typeLiteral != null)
      pattern.add(typeLiteral);

    if (match(Token.Kind.UNARROW) == null)
      throw new RuntimeException("Expected '<-' in object pattern rule");

    if (!(term instanceof Variable))
      throw new RuntimeException(
          "Must select into a valid variable name in object pattern rule: " + term.toSymbol());

    Node rhs = term();
    if (rhs == null)
      throw new RuntimeException("Expected term after '<-' in object pattern rule");
    if (rhs instanceof Variable) {
      // See if we can keep slurping a dot-chain.
      CallChain callChain = new CallChain();
      callChain.add(rhs);
      while (match(Token.Kind.DOT) != null) {
        Node variable = variable();
        if (null == variable)
          throw new RuntimeException("Expected term after '.' in object pattern rule");
        callChain.add(variable);
      }

      rhs = callChain;
    }

    pattern.add(new DestructuringPair(term, rhs));

    while (match(Token.Kind.COMMA) != null) {
      term = variable();
      if (null == term)
        throw new RuntimeException("Expected variable after ',' in object pattern rule");

      if (match(Token.Kind.UNARROW) == null)
        throw new RuntimeException("Expected '<-' in object pattern rule");

      rhs = term();
      if (rhs == null)
        throw new RuntimeException("Expected term after '<-' in object pattern rule");
      if (rhs instanceof Variable) {
        // See if we can keep slurping a dot-chain.
        CallChain callChain = new CallChain();
        callChain.add(rhs);
        while (match(Token.Kind.DOT) != null) {
          Node variable = variable();
          if (null == variable)
            throw new RuntimeException("Expected term after '.' in object pattern rule");
          callChain.add(variable);
        }

        rhs = callChain;
      }

      pattern.add(new DestructuringPair(term, rhs));
    }

    if (match(Token.Kind.RBRACKET) == null) {
      throw new RuntimeException("Expected ']' at end of object pattern");
    }

    return pattern;
  }

  /**
   * argDeclList := LPAREN IDENT (ASSIGN TYPE_IDENT)? (COMMA IDENT (ASSIGN TYPE_IDENT)? )* RPAREN
   */
  private ArgDeclList argDeclList() {
    if (match(Token.Kind.LPAREN) == null) {
      return null;
    }

    List<Token> first = match(Token.Kind.IDENT);
    if (null == first) {
      if (null == match(Token.Kind.RPAREN)) {
        addError("Expected ')' or identifier in function argument list", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }
      return new ArgDeclList();
    }

    List<Token> optionalType = match(Token.Kind.ASSIGN, Token.Kind.TYPE_IDENT);
    ArgDeclList arguments = new ArgDeclList();

    String firstTypeName = optionalType == null ? null : optionalType.get(1).value;
    arguments.add(new ArgDeclList.Argument(first.get(0).value, firstTypeName));

    while (match(Token.Kind.COMMA) != null) {
      List<Token> nextArg = match(Token.Kind.IDENT);
      if (null == nextArg) {
        addError("Expected identifier after ',' in function arguments", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }
      optionalType = match(Token.Kind.ASSIGN, Token.Kind.TYPE_IDENT);
      firstTypeName = optionalType == null ? null : optionalType.get(1).value;

      arguments.add(new ArgDeclList.Argument(nextArg.get(0).value, firstTypeName));
    }

    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' at end of function arguments", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return arguments;
  }

  /**
   * require := REQUIRE IDENT (DOT IDENT)* EOL
   */
  private RequireDecl require() {
    if (match(Token.Kind.REQUIRE) == null) {
      return null;
    }

    List<Token> module = match(Token.Kind.IDENT);
    if (null == module) {
      addError("Expected module identifier", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    List<String> requires = new ArrayList<String>();
    requires.add(module.get(0).value);

    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        addError("Expected module identifier part after '.'", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      requires.add(module.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      addError("Expected newline after require declaration", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return new RequireDecl(requires);
  }

  /**
   * module := MODULE IDENT (DOT IDENT)* EOL
   */
  private ModuleDecl module() {
    if (match(Token.Kind.MODULE) == null) {
      return null;
    }

    List<Token> module = match(Token.Kind.IDENT);
    if (null == module) {
      addError("Expected module identifier after 'module' keyword", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    List<String> modules = new ArrayList<String>();
    modules.add(module.get(0).value);

    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        addError("Expected module identifier part after '.'", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      modules.add(module.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      addError("Expected newline after module declaration", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return new ModuleDecl(modules);
  }


  /*** In-function instruction parsing rules ***/

  /**
   * line := assign
   */
  public Node line() {
    return assign();
  }

  /**
   * Assign a variable an expression.
   *
   * variableAssignment := variable ASSIGN computation
   */
  private Node variableAssignment() {
    List<Token> startTokens = match(Token.Kind.IDENT, Token.Kind.ASSIGN);
    if (null == startTokens)
      return null;

    Node left = new Variable(startTokens.get(0).value);
    Node right = computation();
    if (right == null) {
      addError("Expected expression after ':' in assignment", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    Assignment assignment = new Assignment();
    assignment.add(left);
    assignment.add(right);
    return assignment;
  }

  /**
   * This is really both "free standing expression" and "assignment".
   * <p/>
   * assign := computation (ASSIGN (computation (IF computation | comprehension)?)
   *                        | (IF computation THEN computation ELSE computation) )?
   */
  private Node assign() {
    Node left = computation();
    if (null == left) {
      return null;
    }

    if (match(Token.Kind.ASSIGN) == null) {
      return left;
    }

    // Ternary operator if-then-else
    Node ifThenElse = ternaryIf();
    if (null != ifThenElse) {
      return new Assignment().add(left).add(ifThenElse);
    }

    // OTHERWISE-- continue processing a normal assignment.
    Node right = computation();
    if (null == right) {
      // TODO syntax error, dangling =
      return null;
    }

    // Is this a conditional assignment?
    Node condition = null;
    if (match(Token.Kind.IF) != null) {
      condition = computation();
    } else {
      // Is this a list comprehension?
      Node comprehension = comprehension();
      if (null != comprehension) {
        return new Assignment().add(left).add(right);
      }
    }

    return new Assignment(condition).add(left).add(right);
  }

  /**
   * Ternary operator, like Java's ?:
   * <p/>
   * ternaryIf := IF computation then computation else computation
   */
  private Node ternaryIf() {
    if (match(Token.Kind.IF) != null) {
      Node ifPart = computation();
      if (match(Token.Kind.THEN) == null) {
        addError("IF expression missing THEN clause", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      Node thenPart = computation();
      if (match(Token.Kind.ELSE) == null) {
        addError("IF expression missing ELSE clause", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      Node elsePart = computation();

      return new TernaryExpression()
          .add(ifPart)
          .add(thenPart)
          .add(elsePart);
    }

    return null;
  }

  /**
   * comprehension := FOR variable IN computation (AND computation)?
   */
  private Node comprehension() {
    if (match(Token.Kind.FOR) == null) {
      return null;
    }

    Node variable = variable();
    if (null == variable) {
      addError("Expected variable identifier after 'for' in list comprehension", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    if (match(Token.Kind.IN) == null) {
      addError("Expected 'in' after identifier in list comprehension", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    Node inList = computation();
    if (null == inList) {
      addError("Expected list clause after 'in' in list comprehension", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    if (match(Token.Kind.IF) == null) {
      return new Comprehension(variable, inList, null);
    }

    Node filter = computation();
    if (filter == null) {
      addError("Expected filter expression after 'if' in list comprehension", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return new Comprehension(variable, inList, filter);
  }

  /**
   * group := LPAREN computation RPAREN
   */
  private Node group() {
    if (match(Token.Kind.LPAREN) == null) {
      return null;
    }

    Node computation = computation();
    if (null == computation) {
      addError("Expected expression after '(' in group", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' to close group expression", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return computation;
  }

  /**
   * computation := (group | chain) (comprehension | (rightOp (group | chain)) )*
   */
  public Node computation() {
    Node node = group();

    if (node == null) {
      node = chain();
    }

    // Production failed.
    if (null == node) {
      return null;
    }

    Computation computation = new Computation();
    computation.add(node);

    Node rightOp;
    Node comprehension = null;
    Node operand;
    while ((rightOp = rightOp()) != null || (comprehension = comprehension()) != null) {
      if (comprehension != null) {
        computation.add(comprehension);
        continue;
      }

      operand = group();
      if (null == operand) {
        operand = chain();
      }
      if (null == operand) {
        break;
      }

      rightOp.add(operand);
      computation.add(rightOp);
    }

    return computation;
  }

  /**
   * chain := listOrMapDef | ternaryIf | anonymousFunctionDecl | (term  arglist? (call | indexIntoList)*)
   */
  private Node chain() {
    Node node = listOrMapDef();

    // If not a list, maybe a ternary if?
    if (null == node) {
      node = ternaryIf();
    }

    if (null != node) {
      return node;
    }

    node = anonymousFunctionDecl();
    if (null != node)
      return node;

    // If not an ternary IF, maybe a term?
    node = term();

    // Production failed.
    if (null == node) {
      return null;
    }

    // If args exist, then we should turn this simple term into a free method call.
    CallArguments args = arglist();
    if (null != args) {
      String functionName = (node instanceof Variable)
          ? ((Variable) node).name
          : ((PrivateField) node).name();
      node = new Call(functionName, true, args);
    }

    CallChain chain = new CallChain();
    chain.add(node);

    Node call, indexIntoList = null;
    while ((call = call()) != null || (indexIntoList = indexIntoList()) != null) {
      chain.add(call != null ? call : indexIntoList);
    }

    return chain;
  }

  /**
   * arglist := LPAREN (computation (COMMA computation)*)? RPAREN
   */
  private CallArguments arglist() {
    // Test if there is a leading paren.
    List<Token> parenthetical = match(Token.Kind.LPAREN);

    if (null == parenthetical) {
      return null;
    }

    // Slurp arguments while commas exist.
    CallArguments callArguments;
    // See if this may be a named-arg invocation.
    List<Token> named = match(Token.Kind.IDENT, Token.Kind.ASSIGN);
    boolean isPositional = (null == named);

    callArguments = new CallArguments(isPositional);
    Node arg = computation();
    if (null != arg) {

      // If this is a named arg, wrap it in a name.
      if (isPositional) {
        callArguments.add(arg);
      } else {
        callArguments.add(new CallArguments.NamedArg(named.get(0).value, arg));
      }
    }

    // Rest of argument list, comma separated.
    while (match(Token.Kind.COMMA) != null) {
      named = null;
      if (!isPositional) {
        named = match(Token.Kind.IDENT, Token.Kind.ASSIGN);
        if (null == named) {
          addError("Cannot mix named and positional arguments in a function call", tokens.get(i - 1));
          throw new LoopSyntaxException();
        }
      }

      arg = computation();
      if (null == arg) {
        addError("Expected expression after ',' in function call argument list", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      if (isPositional) {
        callArguments.add(arg);
      } else {
        callArguments.add(new CallArguments.NamedArg(named.get(0).value, arg));
      }
    }

    // Ensure the method invocation is properly closed.
    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' at end of function call argument list", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return callArguments;
  }

  /**
   * An array deref.
   * <p/>
   * indexIntoList := LBRACKET (computation | computation? DOT DOT computation?)? RBRACKET
   */
  private Node indexIntoList() {
    if (match(Token.Kind.LBRACKET) == null) {
      return null;
    }

    Node index = computation();

    // This is a list slice with a range specifier.
    Node to = null;
    boolean slice = false;
    if (match(Token.Kind.DOT) != null) {
      if (match(Token.Kind.DOT) == null) {
        addError("Syntax error, range specifier incomplete. Expected '..'", tokens.get(i - 1));
        throw new LoopSyntaxException();
      }

      slice = true;
      to = computation();
    } else if (index == null) {
      throw new RuntimeException("Expected symbol or '..' list slice operator.");
    }

    if (match(Token.Kind.RBRACKET) == null) {
      addError("Expected ']' at the end of list index expression", tokens.get(i - 1));
      throw new LoopSyntaxException();
    }

    return new IndexIntoList(index, slice, to);
  }

  /**
   * Inline list/map definition.
   * <p/>
   * listOrMapDef := LBRACKET (computation ((COMMA computation)* | computation? DOT DOT computation?))
   *    | (computation COLON computation (COMMA computation COLON computation)*)
   *    | COLON RBRACKET
   */
  private Node listOrMapDef() {
    boolean isBraced = false;
    if (match(Token.Kind.LBRACKET) == null) {
      if (match(Token.Kind.LBRACE) == null) {
        return null;
      } else {
        isBraced = true;
      }
    }

    Node index = computation();

    Node list = new InlineListDef(isBraced);
    if (null != index) {
      boolean isMap = match(Token.Kind.ASSIGN) != null;
      if (isMap) {
        list = new InlineMapDef(isBraced);

        // This map will be stored as a list of alternating keys/values (in pairs).
        list.add(index);
        Node value = computation();
        if (null == value) {
          addError("Expected expression after ':' in map definition", tokens.get(i - 1));
          throw new LoopSyntaxException();
        }
        list.add(value);
      } else {
        list.add(index);
      }

      // Slurp up all list or map argument values as a comma-separated sequence.
      while (match(Token.Kind.COMMA) != null) {
        Node listElement = computation();
        if (null == listElement) {
          addError("Expected expression after ',' in " + (isMap ? "map" : "list") + " definition",
              tokens.get(i - 1));
          throw new LoopSyntaxException();
        }

        list.add(listElement);

        // If the first index contained a hashrocket, then this is a map.
        if (isMap) {
          if (null == match(Token.Kind.ASSIGN)) {
            addError("Expected ':' after map key in map definition", tokens.get(i - 1));
            throw new LoopSyntaxException();
          }

          Node value = computation();
          if (null == value) {
            addError("Expected value expression after ':' in map definition", tokens.get(i - 1));
            throw new LoopSyntaxException();
          }
          list.add(value);
        }
      }


      // OTHERWISE---
      // This is a list slice with a range specifier.
      Node to;
      boolean slice;
      if (match(Token.Kind.DOT) != null) {
        if (match(Token.Kind.DOT) == null) {
          addError("Syntax error, range specifier incomplete. Expected '..'", tokens.get(i - 1));
          throw new LoopSyntaxException();
        }

        slice = true;
        to = computation();
        list = new ListRange(index, slice, to);
      }
    }

    // Is there a hashrocket?
    if (match(Token.Kind.ASSIGN) != null) {
      // This is an empty hashmap.
      list = new InlineMapDef(isBraced);
    }
    if (anyOf(Token.Kind.RBRACKET, Token.Kind.RBRACE) == null) {
      addError("Expected '" + (isBraced ? "}" : "]'"), tokens.get(i - 1));
      return null;
    }

    return list;
  }

  /**
   * A method call production rule.
   * <p/>
   * call := DOT (IDENT | PRIVATE_FIELD) arglist?
   */
  private Node call() {
    List<Token> call = match(Token.Kind.DOT, Token.Kind.IDENT);

    if (null == call)
      call = match(Token.Kind.DOT, Token.Kind.PRIVATE_FIELD);

    // Production failed.
    if (null == call) {
      return null;
    }

    CallArguments callArguments = arglist();

    // Use the ident as name, and it is a method if there are () at end.
    return new Call(call.get(1).value, null != callArguments, callArguments);
  }

  /**
   * term := (literal | variable | field)
   */
  private Node term() {
    Node term = literal();

    if (null == term) {
      term = variable();
    }

    if (null == term) {
      term = field();
    }

    return term;
  }

  /**
   * regexLiteral := DIVIDE ^(DIVIDE|EOL|EOF) DIVIDE
   */
  private Node regexLiteral() {
    int cursor = i;

    Token token = tokens.get(cursor);
    if (Token.Kind.DIVIDE == token.kind) {

      // Look ahead until we find the ending terminal, EOL or EOF.
      boolean noTerminal = false;
      do {
        cursor++;
        token = tokens.get(cursor);
        if (Token.Kind.EOL == token.kind) {
          noTerminal = true;
          break;
        }

      } while (notEndOfRegex(token) && cursor < tokens.size() /* EOF check */);

      // Skip the last divide.
      cursor++;

      if (noTerminal)
        return null;

    } else
      return null;

    int start = i;
    i = cursor;

    // Compress tokens into regex literal.
    StringBuilder builder = new StringBuilder();
    for (Token part : tokens.subList(start, i))
      builder.append(part.value);

    String expression = builder.toString();
    if (expression.startsWith("/") && expression.endsWith("/"))
      expression = expression.substring(1, expression.length() - 1);
    return new RegexLiteral(expression);
  }

  private static boolean notEndOfRegex(Token token) {
    return (Token.Kind.DIVIDE != token.kind && Token.Kind.REGEX != token.kind);
  }

  /**
   * (lexer super rule) literal := string | MINUS? integer | decimal
   *                               | TYPE_IDENT | JAVA_LITERAL
   */
  private Node literal() {
    Token token =
        anyOf(Token.Kind.STRING, Token.Kind.INTEGER, Token.Kind.TYPE_IDENT, Token.Kind.JAVA_LITERAL);
    if (null == token) {
      List<Token> match = match(Token.Kind.MINUS, Token.Kind.INTEGER);
      if (null != match)
        return new IntLiteral('-' + match.get(1).value);
      else
        return null;
    }
    switch (token.kind) {
      case INTEGER:
        return new IntLiteral(token.value);
      case STRING:
        return new StringLiteral(token.value);
      case TYPE_IDENT:
        return new TypeLiteral(token.value);
      case JAVA_LITERAL:
        return new JavaLiteral(token.value);
    }
    return null;
  }

  private Node variable() {
    List<Token> var = match(Token.Kind.IDENT);
    return (null != var) ? new Variable(var.get(0).value) : null;
  }

  private Node field() {
    List<Token> var = match(Token.Kind.PRIVATE_FIELD);
    return (null != var) ? new PrivateField(var.get(0).value) : null;
  }


  /**
   * Right associative operator (see Token.Kind).
   */
  private Node rightOp() {
    if (i >= tokens.size()) {
      return null;
    }
    Token token = tokens.get(i);
    if (isRightAssociative(token)) {
      i++;
      return new BinaryOp(token);
    }

    // No right associative op found.
    return null;
  }

  // Production tools.
  private Token anyOf(Token.Kind... ident) {
    if (i >= tokens.size()) {
      return null;
    }
    for (Token.Kind kind : ident) {
      Token token = tokens.get(i);
      if (kind == token.kind) {
        i++;
        return token;
      }
    }

    // No match =(
    return null;
  }

  private List<Token> match(Token.Kind... ident) {
    int cursor = i;
    for (Token.Kind kind : ident) {

      // What we want is more than the size of the token stream.
      if (cursor >= tokens.size()) {
        return null;
      }

      Token token = tokens.get(cursor);
      if (token.kind != kind) {
        return null;
      }

      cursor++;
    }

    // Forward cursor in token stream to match point.
    int start = i;
    i = cursor;
    return tokens.subList(start, i);
  }

  /**
   * Returns true if this is the end of the text sequence.
   */
  private boolean endOfInput() {
    return i == tokens.size();
  }

  /**
   * Slurps any leading whitespace and returns the count.
   */
  private int withIndent() {
    int indent = 0;
    while (match(Token.Kind.INDENT) != null) {
      indent++;
    }
    return indent;
  }


  public Node ast() {
    return last;
  }

  private static boolean isLeftAssociative(Token token) {
    return null != token && LEFT_ASSOCIATIVE.contains(token.kind);
  }

  private static boolean isRightAssociative(Token token) {
    return null != token && RIGHT_ASSOCIATIVE.contains(token.kind);
  }

  /**
   * recursively walks a parse tree and turns it into a symbolic form that is test-readable.
   */
  public static String stringify(Node tree) {
    StringBuilder builder = new StringBuilder();

    boolean shouldWrapInList = hasChildren(tree);
    if (shouldWrapInList)
      builder.append('(');
    builder.append(tree.toSymbol());

    if (shouldWrapInList)
      builder.append(' ');

    for (Node child : tree.children()) {
      String s = stringify(child);
      if (s.length() == 0)
        continue;

      builder.append(s);
      builder.append(' ');
    }

    // chew last ' '
    if (shouldWrapInList) {
      builder.deleteCharAt(builder.length() - 1);
      builder.append(')');
    }

    return builder.toString();
  }

  private static boolean hasChildren(Node tree) {
    return (null != tree.children()) && !tree.children().isEmpty();
  }
}
