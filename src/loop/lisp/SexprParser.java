package loop.lisp;

import loop.AnnotatedError;
import loop.Parser;
import loop.Token;
import loop.ast.BooleanLiteral;
import loop.ast.ClassDecl;
import loop.ast.InlineListDef;
import loop.ast.IntLiteral;
import loop.ast.LongLiteral;
import loop.ast.Node;
import loop.ast.RegexLiteral;
import loop.ast.StringLiteral;
import loop.ast.Variable;
import loop.ast.script.FunctionDecl;
import loop.ast.script.ModuleDecl;
import loop.ast.script.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class SexprParser implements Parser {
  private final List<Token> tokens;
  private int i;

  public SexprParser(List<Token> tokens) {
    this.tokens = tokens;
  }

  /**
   * Parses an S-expression.
   * <p/>
   * sexpr := atom | (LPAREN sexpr* RPAREN)
   */
  @Override
  public Node parse() {
    InlineListDef list = new InlineListDef(false);
    while (!endOfInput()) {
      Node sexpr = sexpr();
      if (sexpr != null)
        list.add(new SexprAstTransformer(sexpr).transform());
    }
    return list;
  }

  private Node sexpr() {
    Node atom = atom();
    if (atom != null)
      return atom;

    List<Token> startParen = match(Token.Kind.LPAREN);
    if (startParen == null)
      return null;

    InlineListDef listDef = new InlineListDef(false).sourceLocation(startParen);
    Node inner;
    while ((inner = sexpr()) != null) {
      listDef.add(inner);
    }

    if (match(Token.Kind.RPAREN) == null)
      throw new RuntimeException("Expected ')'");

    return listDef;
  }

  /**
   * atom := symbol
   */
  private Node atom() {
    Token token = tokens.get(i);

    if (token.kind == Token.Kind.LPAREN || token.kind == Token.Kind.RPAREN)
      return null;

    i++;

    switch (token.kind) {
      case INTEGER:
        return new IntLiteral(token.value);
      case LONG:
        return new LongLiteral(token.value);
      case STRING:
        return new StringLiteral(token.value);
      case REGEX:
        return new RegexLiteral(token.value);
    }

    if ("#t".equals(token.value) || "#true".equals(token.value))
      return new BooleanLiteral(new Token(token.value, Token.Kind.TRUE, token.line, token.column));
    else if ("#f".equals(token.value) || "#false".equals(token.value))
      return new BooleanLiteral(new Token(token.value, Token.Kind.FALSE, token.line, token.column));

    if (token.value.trim().isEmpty())
      return null;

    return new Variable(token.value);
  }

  @Override
  public Unit script(String file) {
    Unit unit = new Unit(file, ModuleDecl.DEFAULT);
    Node parse = parse();
    for (Node child : parse.children()) {
      if (child instanceof FunctionDecl)
        unit.declare((FunctionDecl) child);
      else
        unit.addToInitializer(child);
    }

    return unit;
  }

  @Override
  public List<AnnotatedError> getErrors() {
    return new ArrayList<AnnotatedError>();
  }

  @Override
  public Node line() {
    return null;
  }

  @Override
  public FunctionDecl functionDecl() {
    return null;
  }

  @Override
  public ClassDecl classDecl() {
    throw new UnsupportedOperationException("The S-expression parser does not support classes");
  }

  private List<Token> match(Token.Kind... ident) {
    return match(false, ident);
  }

  private List<Token> matchNot(Token.Kind... ident) {
    return match(true, ident);
  }

  private List<Token> match(boolean not, Token.Kind... ident) {
    int cursor = i;
    for (Token.Kind kind : ident) {

      // What we want is more than the size of the token stream.
      if (cursor >= tokens.size()) {
        return null;
      }

      Token token = tokens.get(cursor);
      if (not) {
        if (token.kind == kind)
          return null;
      } else if (token.kind != kind) {
        return null;
      }

      cursor++;
    }

    // Forward cursor in token stream to match point.
    int start = i;
    i = cursor;
    return tokens.subList(start, i);
  }

  private boolean endOfInput() {
    return i == tokens.size();
  }
}
