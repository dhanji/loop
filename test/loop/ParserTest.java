package loop;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ParserTest {
  private static final boolean printComparisons = false;

  @Test
  public final void simpleExpr() {
    compare("(= (comput (. x)) (comput (. 1) (+ (. 2))))", "x = 1 + 2");
    compare("(comput (. 1) (+ (. 2)) (+ (. 3)))", "1 + 2 + 3");
    compare("(comput (. 1) (+ (. 2)) (+ (. 3 triple)))", "1 + 2 + 3.triple");

    compare("(comput (. \" hi there! \" to_s) (- (. 1)))", "\" hi there! \".to_s - 1 # yoyoy");

    compare("(comput (. a b c))", "a.b.c");

    // Same thing but this time with a field instead.
    compare("(comput (. @a b c))", "@a.b.c");
  }

  @Test
  public final void javaLiteral() {
    compare("(comput (. `java.lang.System`))", "`java.lang.System`");
  }

  @Test
  public final void callConstructor() {
    compare("(comput (. new Date()))", "new Date()");
    compare("(comput (. new Star(()= name: (comput (. 'Proxima Centauri')) mass: (comput (. 123)))))",
        "new Star(name: 'Proxima Centauri', mass: 123)");
    compare("(comput (. new java.util.Date()))", "new java.util.Date()");
    compare("(comput (. new java.util.HashMap(()= (comput (. map)))))", "new java.util.HashMap({:})");
    compare("(comput (. new loop.MyType(()= a: (comput (. 1)) b: (comput (. 2)))))",
        "new loop.MyType(a: 1, b: 2)");
  }

  @Test
  public final void multilineParentheticalExpr() {
    compare("(comput (comput (. 1) (+ (. 2)) (+ (. 3))))", "(1\n + 2\n + 3)");
    compare("(comput (comput (. 1) (+ (. 2))) (+ (comput (. 3 triple))))",
        "(1 + \n2) + (3.triple\n)");
  }

  @Test
  public final void listIndexing() {
    compare("(comput (. my_list [(comput (. 1))]))", "my_list[1]");
    compare("(comput (. my_list [(comput (. 1))]) (+ (. your_list [(comput (. @idx))])))", "my_list[1] + your_list[@idx]");
    compare("(comput (. my_lt [(comput (. stuff))]) (- (. your_list [(comput (. @idx()))])) (* (. his [(comput (. 32))])))",
        "my_lt[stuff] - your_list[@idx()] * his[32]");

    compare("(comput (. my_list [(comput (. 1))..(comput (. 2))]) (+ (. your_list [(comput (. @idx))..(comput (. y))])))",
        "my_list[1..2] + your_list[@idx..y]");

    compare("(comput (. slice [(comput (. 1))..]) (+ (. yours [..(comput (. slice))])))",
        "slice[1..] + yours[..slice]");
  }

  @Test
  public final void listIndexingAssignment() {
    compare("(= (comput (. numbers [(comput (. 3))..(comput (. 6))])) (comput (. other_list copy() [(comput (. 4))..(comput (. 7))])))",
        "numbers[3..6] = other_list.copy()[4..7]");
  }

  @Test
  public final void listComprehensions() {
    compare("(= (comput (. output)) (comput (. x) (* (. 2)) (cpr for x in (comput (. list)))))",
        "output = x * 2 for x in list");
    compare("(= (comput (. output)) (comput (. x) (* (. 2)) (cpr for x in (comput (. list)) if (comput (. x) (< (. 10))))))",
        "output = x * 2 for x in list if x < 10");
  }

  @Test
  public final void messyListComprehensions() {
    compare("(comput (. (list (comput (. x) (/ (. 2)) (cpr for x in (comput (. (list (comput (. 1)) (comput (. 2)) (comput (. 3)))))" +
        " if (comput (. x) (> (. 2))))))))",
        "[x/2 for x in [1, 2, 3] if x > 2]");
    compare("(comput (comput (. x) (/ (. 2)) (cpr for x in (comput (. (list (comput (. 1)) (comput (. 2)) (comput (. 3)))))" +
        " if (comput (. x) (> (. 2))))))",
        "(x/2 for x in [1, 2, 3] if x > 2)");
    compare("(= (comput (. ls)) (comput (comput (. x) (/ (. 2)) (cpr for x in (comput (. (list (comput (. 1)) (comput (. 2)) (comput (. 3)))))" +
        " if (comput (. x) (> (. 2)))))))",
        "ls = (x/2 for x in [1, 2, 3] if x > 2)");

    // without group.
    compare("(= (comput (. ls)) (comput (. x) (/ (. 2)) (cpr for x in (comput (. (list (comput (. 1)) (comput (. 2)) (comput (. 3)))))" +
        " if (comput (. x) (> (. 2))))))",
        "ls = x/2 for x in [1, 2, 3] if x > 2");
  }

  @Test
  public final void simpleLongerExpr() {
    compare("(comput (. a b c d e))", "a.b.c.d.e");
    compare("(comput (. a b c) (+ (. d e)))", "a.b.c + d.e");
  }

  @Test
  public final void groupExpr() {
    compare("(comput (comput (. a b c)) (+ (. d e)))", "(a.b.c) + d.e");
    compare("(comput (. 1) (+ (comput (. 2) (+ (. 3)))) (+ (. 4)))", "1 + (2 + 3) + 4");
    compare("(comput (. func(()= (comput (comput (. @hi())" +
        " (+ (. (list (comput (comput (. 1) (- (. x [(comput (. 1))])))))))))))" +
        " (+ (comput (. a()) (+ (. (map (comput (. 1)) (comput (comput (. my()) (- (. expr e)))))))))" +
        " (+ (comput (. 4) (- (comput (. 2))))))",
        "func((@hi() + [(1 - x[1])] )) + (a() + {1 : (my() - expr.e)}) + (4 - (2))");
  }

  @Test
  public final void namedArgFunctionCall() {
    compare("(comput (. f(()= name: (comput (. 'dj')) age: (comput (. 29)))))", "f(name: 'dj', age: 29)");
    compare("(comput (. obj fn(()= name: (comput (. 'dj') (+ (. @sur get())))" +
        " age: (comput (. jet age(()= (comput (. 1)) (comput (. 2))))))))",
        "obj.fn(name: 'dj' + @sur.get(), age: jet.age(1, 2))");
  }

  @Test(expected = RuntimeException.class)
  public final void namedAndPositionalArgFunctionCall() {
    compare("(comput (. f(()= name: (comput (. 'dj')) (comput (. 29)))))", "f(name: 'dj', 29)");
  }

  @Test
  public final void simpleLongerExprWithTypeLiterals() {
    compare("(comput (. MyClass b c d e))", "MyClass.b.c.d.e");
    compare("(comput (. a b c) (+ (. YourClass e)) (- (. String)))", "a.b.c + YourClass.e - String");
  }

  @Test
  public final void ternaryIfInFunction() {
    compare("(comput (. func(()= (comput (. 1)) (comput (. (if-then-else (comput (. x)) (comput (. y)) (comput (. z))))))))",
        "func(1, if x then y else z)");
    compare("(comput (. (map (comput (. 1)) (comput (. (if-then-else (comput (. x)) (comput (. y)) (comput (. z))))) (comput (. 2)) (comput (. 12) (+ (. 1))))))",
        "{1 : if x then y else z, 2 : 12 + 1}");
  }

  @Test
  public final void ternaryAssignment() {
    compare("(= (comput (. game)) (if-then-else (comput (. player kick_ass())) (comput (. win)) (comput (. loss))))",
        "game = if player.kick_ass() then win else loss");

    // Repeat with colon instead of =
    compare("(= (comput (. game)) (if-then-else (comput (. player kick_ass())) (comput (. win)) (comput (. loss))))",
        "game: if player.kick_ass() then win else loss");

    compare("(= (comput (. unit)) (if-then-else (comput (. hp) (> (. 15))) (comput (. 'heavy')) (comput (. 'light'))))",
        "unit: if hp > 15 then 'heavy' else 'light'");
  }

  @Test
  public final void conditionalAssignment() {
    compare("(=if(comput (. starcraft)) (comput (. me)) (comput (. happy)))", "me = happy if starcraft");
    compare("(=if(comput (. starcraft playing())) (comput (. me)) (comput (. happy) (+ (. 1))))",
        "me = happy + 1 if starcraft.playing()");

    // Repeat test with COLON as assign operator
    compare("(=if(comput (. starcraft)) (comput (. me)) (comput (. happy)))", "me : happy if starcraft");
    compare("(=if(comput (. starcraft playing())) (comput (. me)) (comput (. happy) (+ (. 1))))",
        "me: happy + 1 if starcraft.playing()");
  }

  @Test
  public final void freeStandingIfThenElse() {
    compare("(comput (. (if-then-else (comput (. x) (> (. 2))) (comput (. do())) (comput (. dont())))))",
        "if x > 2 then do() else dont()");

//    compare("if (comput (. x) (> (. 2))) then (= (comput (. x)) (comput (. 5))) else (= (comput (. x)) (comput (. 10)))",
//        "if x > 2 then\n  x = 5\nelse\n  x = 10");
  }

  @Test
  public final void lists() {
    compare("(comput (. list))", "[]");
    compare("(comput (. (list (comput (. 1)) (comput (. 2)))))", "[1, 2]");
    compare("(comput (. (list (comput (. x y)) (comput (. a) (+ (. 1))) (comput (. b anon()) (/ (. list [(comput (. 1))]))))))",
        "[x.y, a + 1, b.anon() / list[1]]");
    compare("(comput (. (list (comput (. list)) (comput (. list)) (comput (. (list (comput (. list)) (comput (. list))))))))",
        "[[], [], [[], []]]");
  }

  @Test
  public final void sets() {
    compare("(comput (. set))", "{}");
    compare("(comput (. (set (comput (. 1)) (comput (. 2)))))", "{1, 2}");
    compare("(comput (. (set (comput (. x y)) (comput (. a) (+ (. 1))) (comput (. b anon()) (/ (. list [(comput (. 1))]))))))",
        "{x.y, a + 1, b.anon() / list[1]}");
    compare("(comput (. (set (comput (. list)) (comput (. set)) (comput (. (set (comput (. list)) (comput (. list))))))))",
        "{[], {}, {[], []}}");
  }

  @Test
  public final void trees() {
    // An empty treemap just contains a colon between two brackets.
    compare("(comput (. tree))", "[:]");
    compare("(comput (. tree))", "[   :]");
    compare("(comput (. tree))", "[   :  ]");

    compare("(comput (. (tree (comput (. 1)) (comput (. 2)))))", "[1 : 2]");
    compare("(comput (. (tree (comput (. 1)) (comput (. 2)))))", "[1:2]");
    compare("(comput (. (tree (comput (. x y)) (comput (. '22')))))", "[x.y : '22']");
    compare("(comput (. (tree (comput (. x y)) (comput (. '22')))))", "[x.y: '22']");
  }

  @Test
  public final void maps() {
    // An empty hashmap just contains a hashrocket between two brackets.
    compare("(comput (. map))", "{:}");
    compare("(comput (. map))", "{   :}");
    compare("(comput (. map))", "{   :  }");

    compare("(comput (. (map (comput (. 1)) (comput (. 2)))))", "{1 : 2}");
    compare("(comput (. (map (comput (. 1)) (comput (. 2)))))", "{1:2}");
    compare("(comput (. (map (comput (. x y)) (comput (. '22')))))", "{x.y : '22'}");
    compare("(comput (. (map (comput (. x y)) (comput (. '22')))))", "{x.y: '22'}");
  }

  @Test
  public final void messyMaps() {
    compare("(comput " +
        "(. (map " +

        // Pairs:
        "(comput (. 1)) (comput (. 2)) " +
        "(comput (. 2)) (comput (. 3)) " +
        "(comput (. 4)) (comput (. 5 f())) " +
        "(comput (. f())) (comput (. @g) (+ (. 1)))" +
        ")))",

        "{1:2, 2:3, 4: 5.f(), f() : @g + 1}");

    // Assign this map to a variable.
    compare("(= (comput (. map)) (comput " +
        "(. (map " +

        // Pairs:
        "(comput (. 1)) (comput (. 2)) " +
        "(comput (. 2)) (comput (. 3)) " +
        "(comput (. 4)) (comput (. 5 f())) " +
        "(comput (. f())) (comput (. @g) (+ (. 1))))" +
        ")))",

        "map: {1:2, 2:3, 4: 5.f(), f() : @g + 1}");
  }

  @Test
  public final void functionCalls() {
    compare("(comput (. my_var func(()= (comput (. 1)) (comput (. 2)))))", "my_var.func(1, 2)");
    compare("(comput (. y func(()= (comput (. 1) (+ (. x))) (comput (. 2)))))", "y.func(1 + x, 2)");
    compare("(comput (. y func(()= (comput (. 1) (+ (. x))) (comput (. 2 split(()= (comput (. inside) (- (. two))) (comput (. @e))))))))",
        "y.func(1 + x, 2.split(inside - two, @e))");
  }

  @Test
  public final void freeFunctionCalls() {
    compare("(comput (. func(()= (comput (. 1)) (comput (. 2)))))", "func(1, 2)");
    compare("(comput (. do(()= (comput (. 1) (+ (. x))) (comput (. 2)))))", "do(1 + x, 2)");
    compare("(comput (. func(()= (comput (. 1) (+ (. x()))) (comput (. call(()= (comput (. inside) (- (. two))) (comput (. @e))))))))",
        "func(1 + x(), call(inside - two, @e))");
  }

  @Test(expected = RuntimeException.class)
  public final void mapError1() {
    compare("(comput (. map))", "[1::]");
  }

  @Test(expected = RuntimeException.class)
  public final void mapError4() {
    compare("(comput (. map))", "[1:]");
  }

  @Test
  public final void mapError2() {
    expectNoOutput("[::1]");
  }

  @Test(expected = RuntimeException.class)
  public final void mapError3() {
    compare("(comput (. map))", "[1::1, 2]");
  }

  @Test(expected = LoopCompileException.class)
  public final void mapError6() {
    compare("(comput (. map))", "[1:1, 2]");
  }

  @Test
  public final void freeStandingFunctionCall() {
    compare("(comput (. stuff()))", "stuff()");
    compare("(comput (. stuff() more() and_more()))", "stuff().more().and_more()");
    compare("(comput (. stuff()) (+ (. other_stuff() a_bit())))", "stuff() + other_stuff().a_bit()");
  }

  static void compare(String expected, String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Assert.assertNotNull("Parser returned no output", parser.parse());

    if (printComparisons) {
      System.out.println("\n------------------------");
      System.out.println("Parse Tree:\n" + parser.ast());
      System.out.println("Parse S-Expr:\n" + Parser.stringify(parser.ast()));
    }
    Assert.assertEquals(expected, Parser.stringify(parser.ast()));
    if (printComparisons)
      System.out.println("PASS");
  }

  static void expectNoOutput(String input) {
    Parser parser = new Parser(new Tokenizer(input).tokenize());
    Assert.assertNull("Parser returned output!", parser.parse());
  }
}
