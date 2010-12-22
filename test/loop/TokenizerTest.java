package loop;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dhanji R. Prasanna
 */
public class TokenizerTest {

  @Test
  public final void singleLineStatements() {
    compare("\" hi there! \" . to_s - 1", "\" hi there! \".to_s - 1");
    compare("++ 1", "++1");
    compare("1 + 2", "1 +    2");
    compare("x . y + 1 --", "x.y + 1--");
    compare("1 + 2 . double - 2 . 23", "1 +    2.double -2.23");
    compare("1 + 2 . double - 2 . 23 / x ++", "1 +    2.double -2.23 / x++");
    compare("func : ( x , y , z ) -> 'hi'", "func: (x, y, z) -> 'hi'");
    compare("a : ++ 1", "a: ++1");
  }

  @Test
  public final void simpleFunctionCall() {
    compare("print ( 'hello' )", "print('hello')");
    compare("print ( 12 )", "print(12)");
  }

  @Test
  public final void singleLineStatementsWithComments() {
    compare("\" hi there! \" . to_s - 1", "\" hi there! \".to_s - 1 # yoyoy");
    compare("1 + 2", "1 +    2 # + 2");
    compare("x . y + 1 --", "x.y + 1--");
    compare("func : ( x , y , z ) -> \"hi #d\"", "func: (x, y, z) -> \"hi #d\"");
    compare("a : ++ 1", "a: ++1 # pound");
    compare("func : ( x , y , z ) -> 'hi #d'", "func: (x, y, z) -> 'hi #d'");
    compare("", "# + 2");
    compare("~ ~", " # soikdokdpoaksd### 3aoskdoaksd\n ###");
  }

  @Test
  public final void typeNames() {
    List<Token> tokens = new Tokenizer("class MyClass").tokenize();
    Assert.assertEquals(Arrays.asList(
        new Token("class", Token.Kind.CLASS),
        new Token("MyClass", Token.Kind.TYPE_IDENT)),
        tokens);

    tokens = new Tokenizer("class aClass").tokenize();
    Assert.assertFalse(Arrays.asList(
        new Token("class", Token.Kind.CLASS),
        new Token("aClass", Token.Kind.TYPE_IDENT)).equals(tokens));
  }

  @Test
  public final void simpleMultilineStatements() {
    compare("func : ( ) -> \n ~ 'hi'", "func: () -> \n 'hi'");
    compare("func : ( ) -> \n ~ 'hi'", "func: () ->\n 'hi'");
    compare("func : ( x , y ) -> \n ~ 'hi' \n ~ 2", "func: (x,y) ->\n 'hi'\n 2");
    compare("func : -> \n ~ 'hi'", "func: -> \n 'hi'");
  }

  @Test
  public final void compoundMultilineStatements() {
    compare("class Me \n ~ ~ talk : -> \n ~ ~ 'hi'", "class Me \n  talk: ->\n  'hi'");
    compare("class Me \n ~ ~ constructor : -> \n ~ ~ @my : your \n ~ ~ talk : -> \n ~ 'hi'",
            "class Me\n  constructor: ->\n  @my: your\n  talk : -> \n 'hi'");
    compare("class Me \n ~ ~ talk : -> \n ~ ~ 'hi' . to_i 15 , true", 
            "class Me \n  talk: ->\n  'hi'.to_i 15, true");
    compare("class Me extends You , Him \n ~ ~ talk : -> \n ~ ~ 'hi' . to_i ( 15 , true )", 
            "class Me extends You, Him \n  talk: ->\n  'hi'.to_i(15,true)");
  }

  /**
   * COMPARISON LEGEND:
   * <pre>
   * '~' represents leading whitespace.
   * a space on the LHS is a token separator. Everything on the RHS (input arg) is raw input.
   * New lines are rendered as is and only '\n' is treated as newline.
   * '\r' is considered common whitespace.
   * <pre>
   *
   * @param expected the string representation of a tokenized form of the raw string
   * @param input the raw string to be tokenized
   * @throws AssertionError if the tokenized string representation was not an exact match
   *  of the {@code expected} form.
   */
  private static void compare(String expected, String input) {
    List<Token> tokens = new Tokenizer(input).tokenize();
    System.out.println(tokens);
    Assert.assertEquals(expected, Tokenizer.detokenize(tokens));
  }
}
