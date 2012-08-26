package loop;

import loop.Token.Kind;
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
  private final List<AnnotatedError> errors = new ArrayList<AnnotatedError>();
  private final List<Token> tokens;

  private Set<String> aliasedModules;
  private Node last = null;
  private int i = 0;
  private Unit scope;

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
    LEFT_ASSOCIATIVE.add(Token.Kind.UNARROW);
  }

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public Parser(List<Token> tokens, Unit shellScope) {
    this.tokens = tokens;
    this.scope = shellScope;
  }

  public List<AnnotatedError> getErrors() {
    return errors;
  }

  public void addError(String message, Token token) {
    errors.add(new StaticError(message, token));
  }

  public void addError(String message, int line, int column) {
    errors.add(new StaticError(message, line, column));
  }

  /**
   * if := IF computation
   * <p/>
   * assign := computation ASSIGN computation
   * <p/>
   * computation := chain (op chain)+ chain := term call*
   * <p/>
   * call := DOT|UNARROW IDENT (LPAREN RPAREN)?
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
   * script := module? require* (functionDecl | classDecl)* (computation EOL)*
   */
  public Unit script(String file) {
    chewEols();

    ModuleDecl module = module();
    if (null == module)
      module = ModuleDecl.DEFAULT;

    chewEols();

    Unit unit = new Unit(file, module);
    scope = unit;
    RequireDecl require;
    do {
      require = require();
      chewEols();

      if (null != require) {
        if (unit.imports().contains(require) && require.alias == null) {
          addError("Duplicate module import: " + require.toSymbol(),
              require.sourceLine, require.sourceColumn);
          throw new LoopCompileException();
        }

        unit.declare(require);
      }
    } while (require != null);

    FunctionDecl function;
    ClassDecl classDecl = null;
    do {
      function = functionDecl();
      if (function == null) {
        classDecl = classDecl();
      }

      chewEols();

      if (null != function) {
        if (unit.resolveFunction(function.name(), false) != null) {
          addError("Duplicate function definition: " + function.name(),
              function.sourceLine,
              function.sourceColumn);
          throw new LoopCompileException();
        }

        unit.declare(function);
      } else if (null != classDecl) {
        if (unit.getType(classDecl.name) != null) {
          addError("Duplicate type definition: " + classDecl.name,
              classDecl.sourceLine, classDecl.sourceColumn);
          throw new LoopCompileException();
        }

        unit.declare(classDecl);
      }

    } while (function != null || classDecl != null);

    // Now slurp up any freeform expressions into the module initializer.
    Node expression;
    while ((expression = computation()) != null) {
      unit.addToInitializer(expression);
      if (match(Kind.EOL) == null)
        break;
      chewEols();
    }

    chewEols();
    if (i < tokens.size() && errors.isEmpty()) {
      addError("Expected end of script, but additional statements found", tokens.get(i));
      throw new LoopCompileException();
    }

    scope = null;
    return unit;
  }

  private void chewEols() {
    // Chew up end-of-lines.
    //noinspection StatementWithEmptyBody
    while (match(Token.Kind.EOL) != null) ;
  }

  /*** Class parsing rules ***/

  /**
   * Type declaration with inline constructors.
   */
  public ClassDecl classDecl() {
    boolean isImmutable = match(Kind.IMMUTABLE) != null;
    if (match(Kind.CLASS) == null) {
      return null;
    }

    List<Token> className = match(Kind.TYPE_IDENT);
    if (null == className) {
      addError("Expected type identifier (Hint: Types must be upper CamelCase)", tokens.get(i));
      throw new LoopCompileException();
    }

    if (null == match(Kind.ARROW, Kind.LBRACE)) {
      addError("Expected '->' after type identifier", tokens.get(i));
      throw new LoopCompileException();
    }

    ClassDecl classDecl = new ClassDecl(className.iterator().next().value, isImmutable)
        .sourceLocation(className);

    Node line;
    do {
      chewEols();
      withIndent();

      line = line();
      if (line != null) {
        classDecl.add(line);
      }

      chewEols();
    } while (line != null);

    if (!endOfInput() && match(Token.Kind.RBRACE) == null) {
      addError("Expected end of type, additional statements found", tokens.get(i));
      throw new LoopCompileException();
    }

    return classDecl;
  }

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
   * anonymousFunctionDecl := ANONYMOUS_TOKEN argDeclList? 'except' IDENT ARROW EOL (INDENT+ line EOL)
   * <p/>
   * functionDecl := (PRIVATE_FIELD | IDENT) argDeclList? ('in' SYMBOL)? 'except' IDENT ARROW EOL (INDENT+ line EOL)
   * <p/>
   * patternFunctionDecl := (PRIVATE_FIELD | IDENT) argDeclList? ('in' SYMBOL)? 'except' IDENT HASHROCKET EOL (INDENT+ line EOL)*
   */
  private FunctionDecl internalFunctionDecl(boolean anonymous) {
    List<Token> funcName = null;
    List<Token> startTokens = null;
    if (!anonymous) {
      funcName = match(Token.Kind.PRIVATE_FIELD);

      if (null == funcName)
        funcName = match(Token.Kind.IDENT);

      // Not a function
      if (null == funcName) {
        return null;
      }
    } else {
      if ((startTokens = match(Token.Kind.ANONYMOUS_TOKEN)) == null)
        return null;
    }

    // Scan ahead to ensure this is a function decl, coz once we start parsing the arg list
    // we can't go back.
    boolean isFunction = false;
    for (int k = i; k - i < 200 /* panic */ && k < tokens.size(); k++) {
      Token token = tokens.get(k);
      if ((token.kind == Kind.ARROW || token.kind == Kind.HASHROCKET)
          && k < tokens.size() + 1
          && tokens.get(k + 1).kind == Kind.LBRACE) {
        isFunction = true;
        break;
      }
      if (token.kind == Kind.LBRACE || token.kind == Kind.EOL)
        break;
    }

    // Refuse to proceed if there does not appear to be a '->' at the end of the current line.
    if (!isFunction) {
      // Reset the parser in case we've already parsed an identifier.
      i--;
      return null;
    }

    ArgDeclList arguments = argDeclList();
    String name = anonymous ? null : funcName.get(0).value;
    startTokens = funcName != null ? funcName : startTokens;
    FunctionDecl functionDecl = new FunctionDecl(name, arguments).sourceLocation(startTokens);

    // We need to set the module name here because closures are not declared as
    // top level functions in the module.
    if (anonymous)
      functionDecl.setModule(scope.getModuleName());

    // Before we match the start of the function, allow for cell declaration.
    List<Token> inCellTokens = match(Kind.IN, Kind.PRIVATE_FIELD);
    if (inCellTokens != null) {
      functionDecl.cell = inCellTokens.get(1).value;
    }

    // Before we match the arrow and start the function, slurp up any exception handling logic.
    List<Token> exceptHandlerTokens = match(Kind.IDENT, Kind.IDENT);
    if (exceptHandlerTokens == null)
      exceptHandlerTokens = match(Kind.IDENT, Kind.PRIVATE_FIELD);

    if (exceptHandlerTokens != null) {
      Token exceptToken = exceptHandlerTokens.get(0);
      if (!RestrictedKeywords.EXCEPT.equals(exceptToken.value)) {
        addError("Expected 'expect' keyword after function signature", exceptToken);
      }
      functionDecl.exceptionHandler = exceptHandlerTokens.get(1).value;
    }

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

    // Absorb indentation level.
    withIndent();

    boolean hasBody = false;
    while ((line = line()) != null) {
      hasBody = true;
      functionDecl.add(line);

      // Multiple lines are allowed if terminated by a comma.
      if (match(Kind.EOL) == null)
        break;

      // EOLs are optional (probably should discourage this though).
      withIndent();
    }


    if (hasBody) {
      chewEols();

      // Look for a where block attached to this function.
      whereBlock(functionDecl);

      // A function body must be terminated by } (this is ensured by the token-stream rewriter)
      if (!endOfInput() && match(Token.Kind.RBRACE) == null) {
        addError("Expected end of function, additional statements found (did you mean '=>')", tokens.get(i));
        throw new LoopCompileException();
      }
    }

    return functionDecl;
  }

  private FunctionDecl patternMatchingFunctionDecl(FunctionDecl functionDecl) {
    chewEols();
    PatternRule rule = new PatternRule().sourceLocation(functionDecl);
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
      if (pattern == null) {
        List<Token> starToken = match(Kind.STAR);
        if (starToken != null)
          pattern = new WildcardPattern().sourceLocation(starToken);
      }

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

      rule.patterns.add(pattern);
      rule.sourceLocation(pattern);

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
        if (match(Kind.COMMA) == null) {
          if (match(Token.Kind.ASSIGN) == null)
            addError("Expected ':' after pattern", tokens.get(i));
        } else
          continue;

        rule.rhs = line();
        chewEols();
      }

      functionDecl.add(rule);
      rule = new PatternRule().sourceLocation(functionDecl);

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
          functionDecl.declareLocally(helperFunction);
        } else if (null != assignment) {
          hasWhere = true;
          functionDecl.declareLocally(assignment);
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
      throw new LoopCompileException();
    }

    return pattern;
  }

  private Node emptyMapPattern() {
    List<Token> startTokens = match(Kind.LBRACKET, Kind.ASSIGN, Kind.RBRACKET);
    return startTokens != null
        ? new MapPattern().sourceLocation(startTokens) : null;
  }

  private Node emptyListPattern() {
    List<Token> startTokens = match(Kind.LBRACKET, Kind.RBRACKET);
    return startTokens != null
        ? new ListDestructuringPattern().sourceLocation(startTokens) : null;
  }

  /**
   * listOrMapPattern := (LBRACKET term ((ASSIGN term)* | UNARROW term (COMMA term UNARROW term)*)
   * RBRACKET)
   */
  private Node listOrMapPattern() {
    Node pattern;

    // We should allow the possibility of matching a type identifier.
    List<Token> type = match(Token.Kind.TYPE_IDENT);
    TypeLiteral typeLiteral = null;
    if (null != type) {
      typeLiteral = new TypeLiteral(type.get(0).value).sourceLocation(type);
    }

    Token lbracketTokens = anyOf(Kind.LBRACKET, Kind.LBRACE);
    if (lbracketTokens == null)
      return typeLiteral;

    Node term = term();
    if (term == null) {
      addError("Expected term after '[' in pattern rule", tokens.get(i));
      throw new LoopCompileException();
    }

    // This is a list denaturing rule.
    if (match(Token.Kind.ASSIGN) != null) {
      pattern = new ListDestructuringPattern().sourceLocation(lbracketTokens);
      pattern.add(term);
      term = term();
      if (term == null) {
        addError("Expected term after ':' in list pattern rule", tokens.get(i - 1));
        throw new LoopCompileException();
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
      pattern = new ListStructurePattern().sourceLocation(lbracketTokens);
      pattern.add(term);
      if (endList)
        return pattern;

      term = term();
      if (null == term) {
        addError("Expected term after ',' in list pattern rule", tokens.get(i - 1));
        throw new LoopCompileException();
      }
      pattern.add(term);

      while (match(Token.Kind.COMMA) != null) {
        term = term();
        if (null == term) {
          addError("Expected term after ',' in list pattern rule", tokens.get(i - 1));
          throw new LoopCompileException();
        }

        pattern.add(term);
      }

      if (match(Token.Kind.RBRACKET) == null) {
        addError("Expected ']' at end of list pattern rule", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      return pattern;
    }

    // This is a map pattern.
    pattern = new MapPattern().sourceLocation(lbracketTokens);
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
      callChain.nullSafe(false);
      callChain.add(rhs);
      while (match(Token.Kind.DOT) != null) {
        Node variable = variable();
        if (null == variable)
          throw new RuntimeException("Expected term after '.' in object pattern rule");
        callChain.add(variable);
      }

      rhs = callChain;
    }

    pattern.add(new DestructuringPair(term, rhs).sourceLocation(term));

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
        CallChain callChain = new CallChain().sourceLocation(rhs);
        callChain.add(rhs);
        while (match(Token.Kind.DOT) != null) {
          Node variable = variable();
          if (null == variable)
            throw new RuntimeException("Expected term after '.' in object pattern rule");
          callChain.add(variable);
        }

        rhs = callChain;
      }

      pattern.add(new DestructuringPair(term, rhs).sourceLocation(term));
    }

    if (match(Token.Kind.RBRACKET) == null && match(Kind.RBRACE) == null) {
      throw new RuntimeException("Expected '}' at end of object pattern");
    }

    return rewriteObjectPattern(pattern);
  }

  private Node rewriteObjectPattern(Node pattern) {
    for (Node node : pattern.children()) {
      if (node instanceof DestructuringPair) {
        DestructuringPair pair = (DestructuringPair) node;

        // We need to rewrite the chain of variables as a chain of property calls.
        if (pair.rhs instanceof CallChain) {
          CallChain chain = (CallChain) pair.rhs;
          CallChain rewritten = new CallChain();
          rewritten.add(chain.children().remove(0));

          for (Node element : chain.children()) {
            Variable var = (Variable) element;

            rewritten.add(new Dereference(var.name).sourceLocation(var));
          }

          pair.rhs = rewritten;
        }
      }
    }
    return pattern;
  }

  /**
   * argDeclList := LPAREN IDENT (ASSIGN TYPE_IDENT)? (COMMA IDENT (ASSIGN TYPE_IDENT)? )* RPAREN
   */
  private ArgDeclList argDeclList() {
    List<Token> lparenTokens = match(Kind.LPAREN);
    if (lparenTokens == null) {
      return null;
    }

    List<Token> first = match(Token.Kind.IDENT);
    if (null == first) {
      if (null == match(Token.Kind.RPAREN)) {
        addError("Expected ')' or identifier in function argument list", tokens.get(i - 1));
        throw new LoopCompileException();
      }
      return new ArgDeclList().sourceLocation(lparenTokens);
    }

    List<Token> optionalType = match(Token.Kind.ASSIGN, Token.Kind.TYPE_IDENT);
    ArgDeclList arguments = new ArgDeclList().sourceLocation(lparenTokens);

    String firstTypeName = optionalType == null ? null : optionalType.get(1).value;
    arguments.add(new ArgDeclList.Argument(first.get(0).value, firstTypeName));

    while (match(Token.Kind.COMMA) != null) {
      List<Token> nextArg = match(Token.Kind.IDENT);
      if (null == nextArg) {
        addError("Expected identifier after ',' in function arguments", tokens.get(i - 1));
        throw new LoopCompileException();
      }
      optionalType = match(Token.Kind.ASSIGN, Token.Kind.TYPE_IDENT);
      firstTypeName = optionalType == null ? null : optionalType.get(1).value;

      arguments.add(new ArgDeclList.Argument(nextArg.get(0).value, firstTypeName));
    }

    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' at end of function arguments", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    return arguments;
  }

  /**
   * require := REQUIRE IDENT (DOT IDENT)* ('as' IDENT)? EOL
   */
  public RequireDecl require() {
    if (match(Token.Kind.REQUIRE) == null) {
      return null;
    }

    List<Token> module = match(Kind.JAVA_LITERAL);
    if (null == module)
      module = match(Token.Kind.IDENT);
    else {
      if (match(Kind.EOL) == null) {
        addError("Expected newline after require (are you trying to alias Java imports?)",
            tokens.get(i - 1));
        throw new LoopCompileException();
      }

      return new RequireDecl(module.get(0).value).sourceLocation(module);
    }

    if (null == module) {
      addError("Expected module identifier", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    List<String> requires = new ArrayList<String>();
    requires.add(module.get(0).value);

    boolean aliased, javaImport = false;
    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        module = match(Kind.TYPE_IDENT);
        javaImport = true;
      }

      if (null == module) {
        addError("Expected module identifier part after '.'", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      requires.add(module.get(0).value);
    }

    List<Token> asToken = match(Kind.IDENT);
    aliased = asToken != null && RestrictedKeywords.AS.equals(asToken.get(0).value);

    List<Token> aliasTokens = match(Kind.IDENT);
    if (aliased) {
      if (aliasedModules == null)
        aliasedModules = new HashSet<String>();

      if (aliasTokens == null) {
        addError("Expected module alias after '" + RestrictedKeywords.AS + "'", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      // Cache the aliases for some smart parsing of namespaced function calls.
      aliasedModules.add(aliasTokens.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      addError("Expected newline after require declaration", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    // We also allow java imports outside using the backticks syntax.
    String alias = aliased ? aliasTokens.get(0).value : null;
    if (javaImport) {
      return new RequireDecl(requires.toString().replace(", ", "."), alias)
          .sourceLocation(module);
    }

    return new RequireDecl(requires, alias)
        .sourceLocation(module);
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
      throw new LoopCompileException();
    }

    List<String> modules = new ArrayList<String>();
    modules.add(module.get(0).value);

    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        addError("Expected module identifier part after '.'", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      modules.add(module.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      addError("Expected newline after module declaration", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    return new ModuleDecl(modules).sourceLocation(module);
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
   * <p/>
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
      throw new LoopCompileException();
    }

    Assignment assignment = new Assignment();
    assignment.add(left);
    assignment.add(right);
    return assignment.sourceLocation(startTokens);
  }

  /**
   * This is really both "free standing expression" and "assignment".
   * <p/>
   * assign := computation (ASSIGN (computation (IF computation | comprehension)?) | (IF computation
   * THEN computation ELSE computation) )?
   */
  private Node assign() {
    Node left = computation();
    if (null == left) {
      return null;
    }

    List<Token> assignTokens = match(Kind.ASSIGN);
    if (assignTokens == null) {
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
      addError("Expected expression after '=' assign operator", assignTokens.get(0));
      throw new LoopCompileException();
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
    List<Token> ifTokens = match(Kind.IF);
    if (ifTokens == null)
      ifTokens = match(Kind.UNLESS);

    if (ifTokens != null) {
      Token operator = ifTokens.get(0);
      Node ifPart = computation();
      if (match(Token.Kind.THEN) == null) {
        addError(operator.kind + " expression missing THEN clause", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      Node thenPart = computation();

      // Allow user not to specify else (equivalent of "... else Nothing").
      Node elsePart;
      if (match(Token.Kind.ELSE) == null) {
//        addError(operator.kind + " expression missing ELSE clause", tokens.get(i - 1));
//        throw new LoopCompileException();
        elsePart = new TypeLiteral(TypeLiteral.NOTHING);
      } else
        elsePart = computation();

      Node expr = operator.kind == Kind.IF
          ? new TernaryIfExpression()
          : new TernaryUnlessExpression();
      return expr
          .add(ifPart)
          .add(thenPart)
          .add(elsePart)
          .sourceLocation(ifTokens);
    }

    return null;
  }

  /**
   * comprehension := FOR variable IN computation (AND computation)?
   */
  private Node comprehension() {
    List<Token> forTokens = match(Kind.FOR);
    if (forTokens == null) {
      return null;
    }

    Node variable = variable();
    if (null == variable) {
      addError("Expected variable identifier after 'for' in list comprehension", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    if (match(Token.Kind.IN) == null) {
      addError("Expected 'in' after identifier in list comprehension", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    Node inList = computation();
    if (null == inList) {
      addError("Expected list clause after 'in' in list comprehension", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    if (match(Token.Kind.IF) == null) {
      return new Comprehension(variable, inList, null).sourceLocation(forTokens);
    }

    Node filter = computation();
    if (filter == null) {
      addError("Expected filter expression after 'if' in list comprehension", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    return new Comprehension(variable, inList, filter).sourceLocation(forTokens);
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
      throw new LoopCompileException();
    }

    if (match(Token.Kind.RPAREN) == null) {
      addError("Expected ')' to close group expression", tokens.get(i - 1));
      throw new LoopCompileException();
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

    // See if there is a call here.
    MemberAccess postfixCall = call();
    if (postfixCall != null)
      computation.add(postfixCall.postfix(true));

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
   * chain := listOrMapDef | ternaryIf | anonymousFunctionDecl | (term  arglist? (call |
   * indexIntoList)*)
   */
  private Node chain() {
    Node node = listOrMapDef();

    // If not a list, maybe a ternary if?
    if (null == node) {
      node = ternaryIf();
    }

    if (null == node) {
      node = anonymousFunctionDecl();
    }

    // If not an ternary IF, maybe a term?
    if (null == node)
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
      node = new Call(functionName, args).sourceLocation(node);
    }

    CallChain chain = new CallChain();
    chain.add(node);

    // Is this a static method call being set up? I.e. NOT a reference to a constant.
    boolean isJavaStaticRef = node instanceof JavaLiteral && ((JavaLiteral) node).staticFieldAccess == null;
    Node call, indexIntoList = null;
    boolean isFirst = true;
    while ((call = call()) != null || (indexIntoList = indexIntoList()) != null) {
      if (call != null) {
        MemberAccess postfixCall = (MemberAccess) call;
        postfixCall.javaStatic((isFirst && isJavaStaticRef) || postfixCall.isJavaStatic());
        postfixCall.postfix(true);

        // Once we have marked a call as java static, the rest of the chain is not. I.e.:
        // `java.lang.Class`.forName('..').newInstance() <-- the last call is non-static.
        isFirst = false;
      }
      chain.add(call != null ? call : indexIntoList);
    }

    // Smart prediction of whether this is a namespaced call or not.
    List<Node> children = chain.children();
    if (aliasedModules != null
        && !isJavaStaticRef
        && children.size() == 2
        && node instanceof Variable) {
      Variable namespace = (Variable) node;
      if (aliasedModules.contains(namespace.name)) {
        ((Call) children.get(1)).namespace(namespace.name);

        children.remove(0);
      }
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
          addError("Cannot mix named and positional arguments in a function call",
              tokens.get(i - 1));
          throw new LoopCompileException();
        }
      }

      arg = computation();
      if (null == arg) {
        addError("Expected expression after ',' in function call argument list", tokens.get(i - 1));
        throw new LoopCompileException();
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
      throw new LoopCompileException();
    }

    return callArguments;
  }

  /**
   * An array deref.
   * <p/>
   * indexIntoList := LBRACKET (computation | computation? DOT DOT computation?)? RBRACKET
   */
  private Node indexIntoList() {
    List<Token> lbracketTokens = match(Kind.LBRACKET);
    if (lbracketTokens == null) {
      return null;
    }

    Node index = computation();

    // This is a list slice with a range specifier.
    Node to = null;
    boolean slice = false;
    if (match(Token.Kind.DOT) != null) {
      if (match(Token.Kind.DOT) == null) {
        addError("Syntax error, range specifier incomplete. Expected '..'", tokens.get(i - 1));
        throw new LoopCompileException();
      }

      slice = true;
      to = computation();
    } else if (index == null) {
      throw new RuntimeException("Expected symbol or '..' list slice operator.");
    }

    if (match(Token.Kind.RBRACKET) == null) {
      addError("Expected ']' at the end of list index expression", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    return new IndexIntoList(index, slice, to).sourceLocation(lbracketTokens);
  }

  /**
   * Inline list/map definition.
   * <p/>
   * listOrMapDef := LBRACKET (computation ((COMMA computation)* | computation? DOT DOT
   * computation?)) | (computation COLON computation (COMMA computation COLON computation)*) | COLON
   * RBRACKET
   */
  private Node listOrMapDef() {
    boolean isBraced = false;
    List<Token> lbracketTokens = match(Kind.LBRACKET);
    if (lbracketTokens == null) {
      if ((lbracketTokens = match(Token.Kind.LBRACE)) == null) {
        return null;
      } else {
        isBraced = true;
      }
    }

    Node index = computation();

    Node list = new InlineListDef(isBraced).sourceLocation(lbracketTokens);
    if (null != index) {
      boolean isMap = match(Token.Kind.ASSIGN) != null;
      if (isMap) {
        list = new InlineMapDef(!isBraced).sourceLocation(lbracketTokens);

        // This map will be stored as a list of alternating keys/values (in pairs).
        list.add(index);
        Node value = computation();
        if (null == value) {
          addError("Expected expression after ':' in map definition", tokens.get(i - 1));
          throw new LoopCompileException();
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
          throw new LoopCompileException();
        }

        list.add(listElement);

        // If the first index contained a hashrocket, then this is a map.
        if (isMap) {
          if (null == match(Token.Kind.ASSIGN)) {
            addError("Expected ':' after map key in map definition", tokens.get(i - 1));
            throw new LoopCompileException();
          }

          Node value = computation();
          if (null == value) {
            addError("Expected value expression after ':' in map definition", tokens.get(i - 1));
            throw new LoopCompileException();
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
          throw new LoopCompileException();
        }

        slice = true;
        to = computation();
        list = new ListRange(index, slice, to).sourceLocation(lbracketTokens);
      }
    }

    // Is there a hashrocket?
    if (match(Token.Kind.ASSIGN) != null) {
      // This is an empty hashmap.
      list = new InlineMapDef(!isBraced);
    }
    if (anyOf(Token.Kind.RBRACKET, Token.Kind.RBRACE) == null) {
      addError("Expected '" + (isBraced ? "}" : "]'"), tokens.get(i - 1));
      return null;
    }

    return list;
  }

  /**
   *  dereference := '::' (IDENT | TYPE_IDENT) argList?
   */
  private MemberAccess staticCall() {
    List<Token> staticOperator = match(Kind.ASSIGN, Kind.ASSIGN);
    boolean isStatic = RestrictedKeywords.isStaticOperator(staticOperator);
    if (!isStatic)
      return null;

    List<Token> ident = match(Kind.IDENT);

    boolean constant = false;
    if (ident == null) {
      ident = match(Kind.TYPE_IDENT);
      constant = true;
    }

    if (ident == null) {
      addError("Expected static method identifier after '::'", staticOperator.get(0));
      throw new RuntimeException("Expected static method identifier after '::'");
    }

    CallArguments arglist = arglist();

    if (arglist == null)
      return new Dereference(ident.get(0).value)
          .constant(constant)
          .javaStatic(isStatic)
          .sourceLocation(ident);

    // Use the ident as name, and it is a method if there are () at end.
    return new Call(ident.get(0).value, arglist)
        .callJava(true)
        .javaStatic(isStatic)
        .sourceLocation(ident);
  }

  /**
   * A method call production rule.
   * <p/>
   * call := staticCall | (DOT|UNARROW (IDENT | PRIVATE_FIELD) arglist?)
   */
  private MemberAccess call() {
    MemberAccess dereference = staticCall();
    if (dereference != null)
      return dereference;

    List<Token> call = match(Token.Kind.DOT, Token.Kind.IDENT);

    if (null == call)
      call = match(Token.Kind.DOT, Token.Kind.PRIVATE_FIELD);

    boolean forceJava = false, javaStatic = false;
    if (null == call) {
      call = match(Token.Kind.UNARROW, Token.Kind.IDENT);
      forceJava = true;
    }

    // Production failed.
    if (null == call) {
      return null;
    }

    CallArguments callArguments = arglist();
    if (callArguments == null)
      return new Dereference(call.get(1).value)
          .sourceLocation(call);

    // Use the ident as name, and it is a method if there are () at end.
    return new Call(call.get(1).value, callArguments)
        .callJava(forceJava)
        .javaStatic(javaStatic)
        .sourceLocation(call);
  }

  /**
   * constructorCall := NEW TYPE_IDENT arglist
   */
  private Node constructorCall() {
    if (match(Kind.NEW) == null)
      return null;

    String modulePart = null;
    List<Token> module;
    do {
      module = match(Kind.IDENT, Kind.DOT);
      if (module != null) {
        if (modulePart == null)
          modulePart = "";

        modulePart += module.iterator().next().value + ".";
      }
    } while (module != null);

    List<Token> typeName = match(Kind.TYPE_IDENT);
    if (null == typeName) {
      addError("Expected type identifer after 'new'", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    CallArguments arglist = arglist();
    if (null == arglist) {
      addError("Expected '(' after constructor call", tokens.get(i - 1));
      throw new LoopCompileException();
    }

    return new ConstructorCall(modulePart, typeName.iterator().next().value, arglist)
        .sourceLocation(typeName);
  }

  /**
   * term := (literal | variable | field | constructorCall)
   */
  private Node term() {
    Node term = literal();

    if (null == term) {
      term = variable();
    }

    if (null == term) {
      term = field();
    }

    if (null == term) {
      term = constructorCall();
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
    return new RegexLiteral(expression).sourceLocation(token);
  }

  private static boolean notEndOfRegex(Token token) {
    return (Token.Kind.DIVIDE != token.kind && Token.Kind.REGEX != token.kind);
  }

  /**
   * (lexer super rule) literal := string | MINUS? (integer | long | ( integer DOT integer ))
   * | TYPE_IDENT | JAVA_LITERAL
   */
  private Node literal() {
    Token token =
        anyOf(Token.Kind.STRING,
            Token.Kind.INTEGER, Kind.BIG_INTEGER, Kind.LONG,
            Token.Kind.TYPE_IDENT,
            Token.Kind.JAVA_LITERAL,
            Token.Kind.TRUE, Token.Kind.FALSE);
    if (null == token) {
      List<Token> match = match(Token.Kind.MINUS, Token.Kind.INTEGER);
      if (null != match) {
        List<Token> additional = match(Kind.DOT, Kind.INTEGER);
        if (additional != null)
          return new DoubleLiteral('-' + match.get(1).value + '.' + additional.get(1).value)
              .sourceLocation(additional.get(1));

        additional = match(Kind.DOT, Kind.FLOAT);
        if (additional != null)
          return new FloatLiteral('-' + match.get(1).value + '.' + additional.get(1).value + 'F')
              .sourceLocation(additional.get(1));

        return new IntLiteral('-' + match.get(1).value).sourceLocation(match.get(1));
      } else if ((match = match(Kind.MINUS, Kind.LONG)) != null)
        return new LongLiteral('-' + match.get(1).value).sourceLocation(match.get(1));

      else if ((match = match(Kind.MINUS, Kind.BIG_INTEGER)) != null) {
        List<Token> additional = match(Kind.DOT, Kind.INTEGER);
        if (additional != null)
          return new BigDecimalLiteral('-' + match.get(1).value + '.' + additional.get(1).value)
              .sourceLocation(additional.get(1));

        return new BigIntegerLiteral('-' + match.get(1).value).sourceLocation(match.get(1));
      }

      return null;
    }

    switch (token.kind) {
      case TRUE:
      case FALSE:
        return new BooleanLiteral(token).sourceLocation(token);
      case INTEGER:
        List<Token> additional = match(Kind.DOT, Kind.INTEGER);
        if (additional != null)
          return new DoubleLiteral(token.value + '.' + additional.get(1).value)
              .sourceLocation(additional.get(1));

        additional = match(Kind.DOT, Kind.FLOAT);
        if (additional != null)
          return new FloatLiteral(token.value + '.' + additional.get(1).value + 'F')
              .sourceLocation(additional.get(1));

        return new IntLiteral(token.value).sourceLocation(token);
      case BIG_INTEGER:
        additional = match(Kind.DOT, Kind.INTEGER);
        if (additional != null)
          return new BigDecimalLiteral(token.value + '.' + additional.get(1).value)
              .sourceLocation(additional.get(1));

        return new BigIntegerLiteral(token.value).sourceLocation(token);
      case LONG:
        return new LongLiteral(token.value).sourceLocation(token);
      case STRING:
        return new StringLiteral(token.value).sourceLocation(token);
      case TYPE_IDENT:
        return new TypeLiteral(token.value).sourceLocation(token);
      case JAVA_LITERAL:
        return new JavaLiteral(token.value).sourceLocation(token);
    }
    return null;
  }

  private Node variable() {
    List<Token> var = match(Token.Kind.IDENT);
    return (null != var) ? new Variable(var.get(0).value).sourceLocation(var) : null;
  }

  private Node field() {
    List<Token> var = match(Token.Kind.PRIVATE_FIELD);
    return (null != var) ? new PrivateField(var.get(0).value).sourceLocation(var) : null;
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
      return new BinaryOp(token).sourceLocation(token);
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

  public static String stringify(List<Node> list) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0, listSize = list.size(); i < listSize; i++) {
      builder.append(stringify(list.get(i)));

      if (i < listSize - 1)
        builder.append(' ');
    }

    return builder.toString();
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
