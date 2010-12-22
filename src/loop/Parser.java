package loop;

import loop.ast.*;
import loop.ast.script.*;

import java.util.*;

/**
 * Takes the tokenized form of a raw string and converts it
 * to a CoffeeScript parse tree (an optimized form of its AST).
 *
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class Parser {
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

    RIGHT_ASSOCIATIVE.add(Token.Kind.AND);
    RIGHT_ASSOCIATIVE.add(Token.Kind.OR);
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

  /**
   *
   * if := IF computation
   *
   * assign := computation ASSIGN computation
   *
   * computation := chain (op chain)+
   * chain := term call*
   *
   * call := DOT IDENT (LPAREN RPAREN)?
   *
   * term := (literal | variable)
   * literal := (regex | string | number)
   * variable := IDENT
   *
   *
   * Examples
   * --------
   *
   * (assign)
   *
   * x = "hi".tos().tos()
   * x = 1
   *
   * (computation)
   *
   * 1 + 2
   * 1 + 2 - 3 * 4
   * 1.int + 2.y() - 3.a.b * 4
   *
   * --------------------
   *
   * parse := module | require | line
   *
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
   * The top level parsing rule. Do not use parse() to parse entire programs,
   * it is more for one-line expressions.
   *
   * script := module?
   *           require*
   *           (functionDecl | classDecl)*
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
    while (match(Token.Kind.EOL) != null);
  }

  /*** Class parsing rules ***/

  /**
   * functionDecl := IDENT ASSIGN argDeclList? ARROW EOL
   *                 (INDENT+ line EOL)*
   */
  private FunctionDecl functionDecl() {
    List<Token> funcName = match(Token.Kind.IDENT);

    // Not a function
    if (null == funcName) {
      return null;
    }

    ArgDeclList arguments = argDeclList();

    // If it doesn't have an arrow, then it's not a function either.
    if (match(Token.Kind.ARROW, Token.Kind.EOL) == null) {
      return null;
    }

    String name = funcName.get(0).value;

    // Slurp lines into an imperative block. This will form the function body.
    FunctionDecl functionDecl = new FunctionDecl(name, arguments);
    Node line;

    // Absorb indentation level.
    boolean shouldContinue = true;
    do {

      int indent = withIndent();

      boolean eol = match(Token.Kind.EOL) != null;
      if (indent == 0 && !eol) {
        break;
      } else if (eol) {
        // Chew up any blank lines, even those than have indents.
        continue;
      }

      line = line();
      if (line == null) {
        break;
      }

      if (match(Token.Kind.EOL) == null) {
        throw new RuntimeException("Expected newline after statement");
      }

      // TODO: Do something useful with the indent level...
      functionDecl.add(line);
    } while (shouldContinue);

    return functionDecl;
  }

  /**
   * argDeclList := LPAREN
   *                  IDENT (ASSIGN TYPE_IDENT)?
   *                     (COMMA IDENT (ASSIGN TYPE_IDENT)? )*
   *                RPAREN
   */
  private ArgDeclList argDeclList() {
    if (match(Token.Kind.LPAREN) == null) {
      return null;
    }

    List<Token> first = match(Token.Kind.IDENT);
    if (null == first) {
      if (null == match(Token.Kind.RPAREN)) {
        throw new RuntimeException("Expected ')'");
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
        throw new RuntimeException("Expected identifier after ','");
      }
      optionalType = match(Token.Kind.ASSIGN, Token.Kind.TYPE_IDENT);
      firstTypeName = optionalType == null ? null : optionalType.get(1).value;

      arguments.add(new ArgDeclList.Argument(nextArg.get(0).value, firstTypeName));
    }

    if (match(Token.Kind.RPAREN) == null) {
      throw new RuntimeException("Expected ')' at end of argument declaration list");
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
      throw new RuntimeException("Expected module identifier after 'require'");
    }

    List<String> requires = new ArrayList<String>();
    requires.add(module.get(0).value);

    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        throw new RuntimeException("Expected module identifier after '.'");
      }

      requires.add(module.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      throw new RuntimeException("Expected newline after require declaration");
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
      throw new RuntimeException("Expected module identifier after 'require'");
    }

    List<String> modules = new ArrayList<String>();
    modules.add(module.get(0).value);

    while (match(Token.Kind.DOT) != null) {
      module = match(Token.Kind.IDENT);
      if (null == module) {
        throw new RuntimeException("Expected module identifier after '.'");
      }

      modules.add(module.get(0).value);
    }

    if (match(Token.Kind.EOL) == null) {
      throw new RuntimeException("Expected newline after require declaration");
    }

    return new ModuleDecl(modules);
  }


  /*** In-function instruction parsing rules ***/

  /**
   * line := assign
   */
  private Node line() {
    Node parsed = assign();
    if (null == parsed) {
    }
    return parsed;
  }

  /**
   * This is really both "free standing expression" and "assignment".
   *
   * assign := computation
   *      (ASSIGN
   *          (computation (IF computation | comprehension)?)
   *          | (IF computation THEN computation ELSE computation)
   *          )?
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
   *
   *  ternaryIf := IF computation then computation else computation
   */
  private Node ternaryIf() {
    if (match(Token.Kind.IF) != null) {
      Node ifPart = computation();
      if (match(Token.Kind.THEN) == null) {
        throw new RuntimeException("IF with missing THEN");
      }

      Node thenPart = computation();
      if (match(Token.Kind.ELSE) == null) {
        throw new RuntimeException("IF/THEN with missing ELSE");
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
      throw new RuntimeException("Expected identifier");
    }

    if (match(Token.Kind.IN) == null) {
      throw new RuntimeException("Expected 'in' after identifier");
    }

    Node inList = computation();
    if (null == inList) {
      throw new RuntimeException("Expected expression after 'in' in comprehension");
    }

    if (match(Token.Kind.IF) == null) {
      return new Comprehension(variable, inList, null);
    }

    Node filter = computation();
    if (filter == null) {
      throw new RuntimeException("Expected expression after 'and' in comprehension");
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
      throw new RuntimeException("Expected expression after '('");
    }

    if (match(Token.Kind.RPAREN) == null) {
      throw new RuntimeException("Expected ')'");
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
   * chain := listOrMapDef | ternaryIf | (term  arglist? (call | indexIntoList)*)
   */
  private Node chain() {
    Node node = listOrMapDef();

    // If not a list, maybe a ternary if?
    if (null == node) {
      node = ternaryIf();
    }

    // If not a ternary if, maybe a term?
    if (null != node) {
      return node;
    }  else {
      node = term();
    }

    // Production failed.
    if (null == node) {
      return null;
    }

    // If args exist, then we should turn this simple term into a free method call.
    CallArguments args = arglist();
    if (null != args && node instanceof Variable) {
      node = new Call(((Variable)node).name, true, args);
    }

    CallChain chain = new CallChain();
    chain.add(node);

    Node call, indexIntoList = null;
    while ( (call = call()) != null || (indexIntoList = indexIntoList()) != null ) {
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

    boolean isParenthetical = (null != parenthetical);
    boolean isPositional = true;

    // Slurp arguments while commas exist.
    CallArguments callArguments = null;
    if (isParenthetical) {

      // See if this may be a named-arg invocation.
      List<Token> named = match(Token.Kind.IDENT, Token.Kind.ASSIGN);
      isPositional = (null == named);

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
    }

    // Rest of argument list, comma separated.
    while (isParenthetical && match(Token.Kind.COMMA) != null) {
      List<Token> named = null;
      if (!isPositional) {
        named = match(Token.Kind.IDENT, Token.Kind.ASSIGN);
        if (null == named) {
          throw new RuntimeException("Cannot mix named and position arguments in a function call");
        }
      }

      Node arg = computation();
      if (null == arg) {
        throw new RuntimeException("Expected expression after ','");
      }

      if (isPositional) {
        callArguments.add(arg);
      } else {
        callArguments.add(new CallArguments.NamedArg(named.get(0).value, arg));
      }
    }

    // Ensure the method invocation is properly closed.
    if (isParenthetical && match(Token.Kind.RPAREN) == null) {
      throw new RuntimeException("Expected ')' at end of argument list");
    }

    return callArguments;
  }

  /**
   * An array deref.
   *
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
        throw new RuntimeException("Syntax error, range specifier incomplete. Expected '..'");
      }

      slice = true;
      to = computation();
    } else if (index == null) {
      throw new RuntimeException("Expected symbol or '..' list slice operator.");
    }

    if (match(Token.Kind.RBRACKET) == null) {
      throw new RuntimeException("Expected ]");
    }

    return new IndexIntoList(index, slice, to);
  }

  /**
   * Inline list/map definition.
   *
   * listOrMapDef :=
   *      LBRACKET
   *          (computation
   *              ((COMMA computation)* | computation? DOT DOT computation?))
   *          |
   *          (computation HASHROCKET computation
   *              (COMMA computation HASHROCKET computation)*)
   *          |
   *          HASHROCKET
   *      RBRACKET
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
      boolean isMap = match(Token.Kind.ASSIGN, Token.Kind.GREATER) != null;
      if (isMap) {
        list = new InlineMapDef(isBraced);

        // This map will be stored as a list of alternating keys/values (in pairs).
        list.add(index);
        Node value = computation();
        if (null == value) {
          throw new RuntimeException("Expected expression after '=>'");
        }
        list.add(value);
      } else {
        list.add(index);
      }

      // Slurp up all list or map argument values as a comma-separated sequence.
      while (match(Token.Kind.COMMA) != null) {
        Node listElement = computation();
        if (null == listElement) {
          throw new RuntimeException("Expected expression after ','");
        }

        list.add(listElement);

        // If the first index contained a hashrocket, then this is a map.
        if (isMap) {
          if (null == match(Token.Kind.ASSIGN, Token.Kind.GREATER)) {
            throw new RuntimeException("Expected '=>' after key");
          }
          
          Node value = computation();
          if (null == value) {
            throw new RuntimeException("Expected expression after '=>'");
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
          throw new RuntimeException("Syntax error, range specifier incomplete. Expected '..'");
        }

        slice = true;
        to = computation();
        list = new ListRange(index, slice, to);
      }
    }

    // Is there a hashrocket?
    if (match(Token.Kind.ASSIGN, Token.Kind.GREATER) != null) {
      // Otherwise this is an empty hashmap.
      list = new InlineMapDef(isBraced);
    }
    if (anyOf(Token.Kind.RBRACKET, Token.Kind.RBRACE) == null) {
      throw new RuntimeException("Expected " + (isBraced ? "}" : "]"));
    }

    return list;
  }

  /**
   * A method call production rule.
   *
   * call := DOT IDENT arglist?
   */
  private Node call() {
    List<Token> call = match(Token.Kind.DOT, Token.Kind.IDENT);

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
   * (lexer super rule)
   * literal := string | regex | integer | decimal
   */
  private Node literal() {
    Token token = anyOf(Token.Kind.STRING, Token.Kind.INTEGER, Token.Kind.REGEX, Token.Kind.TYPE_IDENT);
    if (null == token) {
      return null;
    }
    switch (token.kind) {
      case INTEGER:
        return new IntLiteral(token.value);
      case STRING:
        return new StringLiteral(token.value);
      case TYPE_IDENT:
        return new TypeLiteral(token.value);
      case REGEX:
        return null; // TODO fix.
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
   * recursively walks a parse tree and turns it into a symbolic form
   * that is test-readable.
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
