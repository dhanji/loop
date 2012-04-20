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
        "(class Person -> (= (comput (. name)) (comput (. 'dude'))))",
        "class Person ->\n  name: 'dude'\n");
  }

  @Test
  public final void immutableTypeDeclaration() {
    compareType("Person",
        "(immutable_class Person -> (= (comput (. name)) (comput (. 'dude'))))",
        "immutable class Person ->\n  name: 'dude'\n");
  }

  @Test
  public final void multilineTypeDeclaration() {
    compareType("Person",
        "(class Person ->" +
            " (= (comput (. name)) (comput (. 'dude')))" +
            " (= (comput (. age)) (comput (. 25)))" +
            " (= (comput (. weight)) (comput (. 194)))" +
            " (= (comput (. cool)) (comput (. true))))",
        "class Person ->" +
            "\n  name: 'dude'" +
            "\n  age: 25" +
            "\n  weight: 194" +
            "\n  cool: true"
    );
  }

  @Test
  public final void multilineTypeDeclarationWithSomeDefaults() {
    compareType("Person",
        "(class Person ->" +
            " (comput (. age))" +
            " (= (comput (. name)) (comput (. 'Guy')))" +
            " (comput (. weight))" +
            " (= (comput (. cool)) (comput (. true))))",
        "class Person ->" +
            "\n  age" +
            "\n  name: 'Guy'" +
            "\n  weight" +
            "\n  cool: true"
    );
  }

  static void compareType(String typeName, String expected, String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Unit unit = parser.script(null);
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
