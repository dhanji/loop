package loop;

import loop.Token.Kind;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
    compare("func : ( x , y , z ) -> { 'hi' }", "func: (x, y, z) -> 'hi'");
    compare("a : ++ 1", "a: ++1");
  }

  @Test
  public final void simpleFunctionCall() {
    compare("print ( 'hello' )", "print('hello')");
    compare("print ( 12 )", "print(12)");
  }

  @Test
  public final void regexAndStringLiterals() {
    compare("2 . 0 'hello' \"hi\" / yo / 123", "2.0 'hello' \"hi\" /yo/ 123");
    compare("2 . 0 'hello' \"hi\" / yo */ 123", "2.0 'hello' \"hi\" /yo*/ 123");
  }

  @Test
  public final void numericLiterals() {
    compare("2 . 0 @22 22L", "2.0 @22 22L");

    assertEquals(Arrays.asList(
        new Token("2", Kind.INTEGER, 0, 0),
        new Token(".", Kind.DOT, 0, 0),
        new Token("0", Kind.INTEGER, 0, 0),
        new Token("@22", Kind.BIG_INTEGER, 0, 0),
        new Token("22L", Kind.LONG, 0, 0)
    ), new Tokenizer("2.0 @22 22L").tokenize());
  }

  @Test
  public final void regexAndStringAndJavaLiterals() {
    compare("2 . 0 'hello' \"hi\" / yo / 123 `java.lang.System`",
        "2.0 'hello' \"hi\" /yo/ 123 `java.lang.System`");
  }

  @Test
  public final void singleLineStatementsWithComments() {
    compare("\" hi there! \" . to_s - 1", "\" hi there! \".to_s - 1 # yoyoy");
    compare("1 + 2", "1 +    2 # + 2");
    compare("x . y + 1 --", "x.y + 1--");
    compare("func : ( x , y , z ) -> { \"hi #d\" }", "func: (x, y, z) -> \"hi #d\"");
    compare("a : ++ 1", "a: ++1 # pound");
    compare("func : ( x , y , z ) -> { 'hi #d' }", "func: (x, y, z) -> 'hi #d'");
    compare("", "# + 2");
    compare("~ ~", " # soikdokdpoaksd### 3aoskdoaksd\n ###");
  }

  @Test
  public final void typeNames() {
    List<Token> tokens = new Tokenizer("class MyClass").tokenize();
    Assert.assertEquals(Arrays.asList(
        new Token("class", Token.Kind.CLASS, 0, 0),
        new Token("MyClass", Token.Kind.TYPE_IDENT, 0, 0)),
        tokens);

    tokens = new Tokenizer("class aClass").tokenize();
    Assert.assertFalse(Arrays.asList(
        new Token("class", Token.Kind.IDENT, 0, 0),
        new Token("aClass", Token.Kind.TYPE_IDENT, 0, 0)).equals(tokens));

    tokens = new Tokenizer("immutable class aClass").tokenize();
    Assert.assertFalse(Arrays.asList(
        new Token("immutable", Token.Kind.IMMUTABLE, 0, 0),
        new Token("class", Token.Kind.IDENT, 0, 0),
        new Token("aClass", Token.Kind.TYPE_IDENT, 0, 0)).equals(tokens));
  }

  @Test
  public final void simpleMultilineStatements() {
    compare("func : ( ) -> { \n ~ 'hi' }", "func: () -> \n 'hi'");
    compare("func : ( ) -> { \n ~ 'hi' }", "func: () ->\n 'hi'");
    compare("func : ( x , y ) -> { \n ~ 'hi' \n ~ 2 }", "func: (x,y) ->\n 'hi'\n 2");
    compare("func : -> { \n ~ 'hi' }", "func: -> \n 'hi'");
  }

  @Test
  public final void multilineWithGroupNewlineElision() {
    compare("func : ( ) -> { \n ~ ( 1 + 2 ) }", "func: () -> \n (1 \n + 2)");
    compare("func : ( ) -> { \n ~ ( 1 + 2 ) }", "func: () -> \n (\n1 \n +\n 2\n\n)");

    // Do not elide newlines that occur in groups that symbolize anonymous functions.
    compare("func : ( ) -> { \n ~ ( @ ( ) -> { 2 } ) }", "func: () -> \n (@() ->\n  2)");
    compare("func ( x , y , z ) -> { \n" +
        " ~ ~ @ ( ) -> { \n" +
        " ~ ~ ~ ~ 1 + 2 . toString ( ) \n" +
        " } }", "func (x, y, z) ->\n  @() ->\n    1 + 2.toString()\n");
  }

  @Test
  public final void multilineNestedFunctions() {
    compare("func ( x , y , z ) -> { \n" +
        " ~ ~ @ ( ) -> { \n" +
        " ~ ~ ~ ~ 1 + 2 . toString ( ) \n" +
        " } } \n" +
        " func2 -> { \n" +
        " ~ ~ answer }", "func (x, y, z) ->\n  @() ->\n    1 + 2.toString()\n\nfunc2 ->\n  answer");
  }

  @Test
  public final void multilineWithGroupNewlineElisionPatternFunction() {
    compare("func : ( ) => { \n ~ ( 1 + 2 ) }", "func: () => \n (1 \n + 2)");
  }

  @Test
  public final void stringWithTrailineWhitespRegression() {
    compare("func : ( ) => { \n ~ 'hello' + 1 }", "func: () => \n 'hello'     + 1");
  }

  @Test
  public final void compoundMultilineStatements() {
    compare("class Me \n ~ ~ talk : -> { \n ~ ~ 'hi' }", "class Me \n  talk: ->\n  'hi'");
    compare("class Me \n ~ ~ constructor : -> { \n ~ ~ @my : your \n ~ ~ talk : -> { \n ~ 'hi' } }",
            "class Me\n  constructor: ->\n  @my: your\n  talk : -> \n 'hi'");
    compare("class Me \n ~ ~ talk : -> { \n ~ ~ 'hi' . to_i 15 , true }",
            "class Me \n  talk: ->\n  'hi'.to_i 15, true");
    compare("class Me extends You , Him \n ~ ~ talk : -> { \n ~ ~ 'hi' . to_i ( 15 , true ) }",
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
