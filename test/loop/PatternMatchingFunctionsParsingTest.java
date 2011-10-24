package loop;

import org.junit.Test;

import static loop.FreeFunctionsParsingTest.compareFunction;


/**
 * NOTE: Pattern matching functions all start with => rather than ->
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class PatternMatchingFunctionsParsingTest {

  @Test
  public final void reverseListInPatternMatchingForm() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput list) \n" +
        "  => ([] x xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  [x:xs]      : reverse(xs) + x");
  }

  @Test
  public final void listPatternMatchingMultipleSegments() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput list) \n" +
        "  => ([] x xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))) \n" +
        "  => ([] x y xs ys) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  [x:xs]      : reverse(xs) + x\n" +
            "  [x:y:xs:ys]      : reverse(xs) + x");
  }

  @Test
  public final void listPatternMatchingMixedSegments() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput list) \n" +
        "  => ([] 'hello x' xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))) \n" +
        "  => ([] 0 x 1 xs ys) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  ['hello x':xs]      : reverse(xs) + x\n" +
            "  [0:x:1:xs:ys]       : reverse(xs) + x");
  }

  @Test
  public final void listPatternMatchingOtherwise() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput list) \n" +
        "  => ([] 'hello x' xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))) \n" +
        "  => otherwise : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  ['hello x':xs]      : reverse(xs) + x\n" +
            "  otherwise           : reverse(xs) + x");
  }

  @Test
  public final void mapPatternMatchingSimple() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [::] : (comput (. 1)) \n" +
        "  => ([::] name <- first) : (comput (. 2)) \n" +
        "  => otherwise : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  [::]          : 1\n" +
            "  [name <- first]      : 2\n" +
            "  otherwise            : -1");
  }

  @Test
  public final void mapPatternMatchingManyDestructures() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput (. 0)) \n" +
        "  => [::] : (comput (. 1)) \n" +
        "  => ([::] name <- first) : (comput (. 3)) \n" +
        "  => ([::] name <- first age <- second) : (comput (. 2)) \n" +
        "  => otherwise : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  []                     : 0\n" +
            "  [::]                   : 1\n" +
            "  [name <- first]        : 3\n" +
            "  [name <- first," +
            "   age  <- second]       : 2\n" +
            "  otherwise              : -1");
  }
}
