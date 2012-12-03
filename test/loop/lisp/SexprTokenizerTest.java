package loop.lisp;

import loop.Token;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class SexprTokenizerTest {
  @Test
  public final void simpleSExpressions() {
    assertEquals(Arrays.asList(
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("+", Token.Kind.PLUS, 0, 0),
        new Token("1", Token.Kind.INTEGER, 0, 0),
        new Token("2", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0)),
        new SexprTokenizer("(+ 1 2)").tokenize());

    assertEquals(Arrays.asList(
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("+", Token.Kind.PLUS, 0, 0),
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("1", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0),
        new Token("2", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0)),
        new SexprTokenizer("(+ (1) 2)").tokenize());
  }

  @Test
  public final void simpleSExpressionsWithComments() {
    assertEquals(Arrays.asList(
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("+", Token.Kind.PLUS, 0, 0),
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("1", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0),
        new Token("2", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0)),
        new SexprTokenizer("(+ (1) 2) ;hello").tokenize());
    assertEquals(Arrays.asList(
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("+", Token.Kind.PLUS, 0, 0),
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("1", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0),
        new Token("2", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0)),
        new SexprTokenizer("(+ ;dude \n (1) 2);hello").tokenize());
    assertEquals(Arrays.asList(
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("+", Token.Kind.PLUS, 0, 0),
        new Token("(", Token.Kind.LPAREN, 0, 0),
        new Token("1", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0),
        new Token("2", Token.Kind.INTEGER, 0, 0),
        new Token(")", Token.Kind.RPAREN, 0, 0)),
        new SexprTokenizer("(+ # dude \n (1) 2);hello").tokenize());
  }
}
