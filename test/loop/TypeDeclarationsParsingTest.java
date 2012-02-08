package loop;

import loop.ast.ClassDecl;
import loop.ast.script.Unit;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for types declared in the top-lexical space of a script.
 */
public class TypeDeclarationsParsingTest {

  @Test
  public final void simpleTypeDeclaration() {
    compareType("Person",
        "(class Person -> (= (comput (. name[String])) (comput (. 'dude'))))",
        "class Person ->\n  String name: 'dude'\n");
  }

  @Test
  public final void multilineTypeDeclaration() {
    compareType("Person",
        "(class Person ->" +
            " (= (comput (. name[String])) (comput (. 'dude')))" +
            " (= (comput (. age[Integer])) (comput (. 25)))" +
            " (= (comput (. weight[Integer])) (comput (. 194)))" +
            " (= (comput (. cool[Boolean])) (comput (. true))))",
        "class Person ->" +
            "\n  String name: 'dude'" +
            "\n  Integer age: 25" +
            "\n  Integer weight: 194" +
            "\n  Boolean cool: true"
    );
  }

  @Test
  public final void multilineTypeDeclarationWithSomeDefaults() {
    compareType("Person",
        "(class Person ->" +
            " (comput (. age[Integer]))" +
            " (= (comput (. name[String])) (comput (. 'Guy')))" +
            " (comput (. weight[Integer]))" +
            " (= (comput (. cool[Boolean])) (comput (. true))))",
        "class Person ->" +
            "\n  Integer age" +
            "\n  String name: 'Guy'" +
            "\n  Integer weight" +
            "\n  Boolean cool: true"
    );
  }

  static void compareType(String typeName, String expected, String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Unit unit = parser.script();
    Assert.assertNotNull("Parser returned no output", unit);

    ClassDecl clazz = unit.getType(typeName);
    Assert.assertNotNull("No such type " + typeName, clazz);


    String stringified = Parser.stringify(clazz);

    System.out.println("\n------------------------");
    System.out.println("Parse Tree:\n" + typeName);
    System.out.println("Parse S-Expr:\n" + stringified);
    Assert.assertEquals(expected, stringified);
    System.out.println("PASS");
  }
}
