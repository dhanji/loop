package loop;

import loop.ast.Node;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests reduced ASTs similar to the ParserTest
 */
public class ReducerTest {

  @Test
  public final void simpleExpr() {
    compare("(= x (comput 1 (+ 2)))", "x = 1 + 2");
    compare("(comput 1 (+ 2) (+ 3))", "1 + 2 + 3");
    compare("(comput 1 (+ 2) (+ (. 3 triple)))", "1 + 2 + 3.triple");

    compare("(comput (. \" hi there! \" to_s) (- 1))", "\" hi there! \".to_s - 1 # yoyoy");

    compare("(. a b c)", "a.b.c");

    // Same thing but this time with a field instead.
    compare("(. @a b c)", "@a.b.c");
  }

  @Test
  public final void listIndexing() {
    compare("(. my_list [1])", "my_list[1]");
    compare("(comput (. my_list [1]) (+ (. your_list [@idx])))", "my_list[1] + your_list[@idx]");
    compare("(comput (. my_lt [stuff]) (- (. your_list [@idx])) (* (. his [32])))",
        "my_lt[stuff] - your_list[@idx] * his[32]");

    compare("(comput (. my_list [1..2]) (+ (. your_list [@idx..y])))",
        "my_list[1..2] + your_list[@idx..y]");

    compare("(comput (. slice [1..]) (+ (. yours [..slice])))",
        "slice[1..] + yours[..slice]");

    // Sanity check that it doesnt reduce too much!
    compare("(. slice [(comput 1 (+ yours))..(comput 3 (- theirs))])",
        "slice[1 + yours..3 - theirs]");
  }

  @Test
  public final void listIndexingAssignment() {
    compare("(= (. numbers [3..6]) (. other_list copy() [4..7]))",
        "numbers[3..6] = other_list.copy()[4..7]");
  }

  @Test
  public final void listComprehensions() {
    compare("(= output (comput (cpr x (* 2) for x in list)))",
        "output = x * 2 for x in list");
    compare("(= output (comput (cpr x (* 2) for x in list if (comput x (< 10)))))",
        "output = x * 2 for x in list if x < 10");

    // Sanity check
    compare("(= output (comput (cpr x (* 2) for x in (comput list1 (+ list2)) if (comput x (< 10)))))",
        "output = x * 2 for x in list1 + list2 if x < 10");
  }

  @Test
  public final void messyListComprehensions() {
    compare("(. (list (comput (cpr x (/ 2) for x in (list 1 2 3) if (comput x (> 2))))))",
        "[x/2 for x in [1, 2, 3] if x > 2]");
    compare("(comput (comput (cpr x (/ 2) for x in (list 1 2 3) if (comput x (> 2)))) (+ y))",
        "(x/2 for x in [1, 2, 3] if x > 2) + y");
    compare("(= ls (comput (cpr x (/ 2) for x in (list 1 2 3) if (comput x (> 2)))))",
        "ls = (x/2 for x in [1, 2, 3] if x > 2)");

    // without group (reduces to the same).
    compare("(= ls (comput (cpr x (/ 2) for x in (list 1 2 3) if (comput x (> 2)))))",
        "ls = x/2 for x in [1, 2, 3] if x > 2");
  }

  @Test
  public final void simpleLongerExpr() {
    compare("(. a b c d e)", "a.b.c.d.e");
    compare("(comput (. a b c) (+ (. d e)))", "a.b.c + d.e");
  }

  static void compare(String expected, String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    parser.parse();

    // Need to stringify first coz the reducer mutates the original AST.
    String parsedSexpr = Parser.stringify(parser.ast());

    Node reduced = new Reducer(parser.ast()).reduce();
    Assert.assertNotNull("Reducer returned no output", reduced);

    System.out.println("\n------------------------");
    System.out.println("Reduced Parse Tree:\n" + reduced);
    System.out.println("Parsed S-Expr:   " + parsedSexpr);
    System.out.println("Reduced S-Expr:  " + Parser.stringify(reduced));
    Assert.assertEquals(expected, Parser.stringify(reduced));
    System.out.println("PASS");
  }
}
