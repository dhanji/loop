package loop.lisp;

import loop.AnnotatedError;
import loop.Parser;
import loop.Token;
import loop.ast.ClassDecl;
import loop.ast.InlineListDef;
import loop.ast.Node;
import loop.ast.Variable;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;

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
   *
   * sexpr := atom | (LPAREN sexpr* RPAREN)
   */
  @Override
  public Node parse() {
    Node atom = atom();
    if (atom != null)
      return atom;

    List<Token> startParen = match(Token.Kind.LPAREN);
    if (startParen == null)
      return null;

    InlineListDef listDef = new InlineListDef(false).sourceLocation(startParen);
    Node inner;
    while ((inner = parse()) != null) {
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
    return new Variable(token.value);
  }

  @Override
  public Unit script(String file) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<AnnotatedError> getErrors() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Node line() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public FunctionDecl functionDecl() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
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
