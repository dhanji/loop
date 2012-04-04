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
        "  => [] : (comput (. list)) \n" +
        "  => ([] x xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  [x:xs]      : reverse(xs) + x");
  }

  @Test
  public final void listPatternMatchingMultipleSegments() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput (. list)) \n" +
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
        "  => [] : (comput (. list)) \n" +
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
        "  => [] : (comput (. list)) \n" +
        "  => ([] 'hello x' xs) : (comput (. reverse(()= (comput (. xs)))) (+ (. x))) \n" +
        "  => wildcard : (comput (. reverse(()= (comput (. xs)))) (+ (. x))))",
        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  ['hello x':xs]      : reverse(xs) + x\n" +
            "  *                   : reverse(xs) + x");
  }

  @Test
  public final void mapPatternMatchingSimple() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [::] : (comput (. 1)) \n" +
        "  => ([::] name <- (. first)) : (comput (. 2)) \n" +
        "  => wildcard : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  [:]                  : 1\n" +
            "  [name <- first]      : 2\n" +
            "  *                    : -1");
  }

  @Test
  public final void mapPatternMatchingManyDestructures() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput (. 0)) \n" +
        "  => [::] : (comput (. 1)) \n" +
        "  => ([::] name <- (. first)) : (comput (. 3)) \n" +
        "  => ([::] name <- (. first) age <- (. second)) : (comput (. 2)) \n" +
        "  => wildcard : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  []                     : 0\n" +
            "  [:]                    : 1\n" +
            "  [name <- first]        : 3\n" +
            "  [name <- first," +
            "   age  <- second]       : 2\n" +
            "  *                      : -1");
  }

  @Test
  public final void mapPatternMatchingNestedDestructures() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput (. 0)) \n" +
        "  => [::] : (comput (. 1)) \n" +
        "  => ([::] dad <- (. parent name)) : (comput (. dad)) \n" +
        "  => wildcard : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  []                     : 0\n" +
            "  [:]                    : 1\n" +
            "  [ dad <- parent.name ]       : dad\n" +
            "  *                      : -1");
  }

  @Test
  public final void literalPatternMatching() {
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => 0 : (comput (. 1)) \n" +
        "  => 'hello' : (comput (. 2)) \n" +
        "  => wildcard : (comput (. -1)))",
        "reverse(list) =>\n" +
            "  0            : 1\n" +
            "  'hello'      : 2\n" +
            "  *            : -1");
  }

  @Test
  public final void basicTypePatternMatching() {
    compareFunction("handle",
        "(handle: (()= req) -> \n" +
        "  => HttpRequest : (comput (. req param(()= (comput (. 'stuff'))))) \n" +
        "  => FtpRequest : (comput (. req param(()= (comput (. 'buff'))))))",
        "handle(req) =>\n" +
            "  HttpRequest    : req.param('stuff')\n" +
            "  FtpRequest     : req.param('buff')\n");
  }

  @Test
  public final void objectTypePatternMatching() {
    compareFunction("handle",
        "(handle: (()= req) -> \n" +
            "  => ([::] HttpRequest ip <- (. ip)) : (comput (. ip)) \n" +
            "  => ([::] HttpRequest name <- (. params name)) : (comput (. name)) \n" +
            "  => FtpRequest : (comput (. req param(()= (comput (. 'buff'))))))",
        "handle(req) =>\n" +
            "  HttpRequest[ip <- ip]              : ip\n" +
            "  HttpRequest[name <- params.name]   : name\n" +
            "  FtpRequest              : req.param('buff')\n");
  }

  @Test
  public final void guardedObjectTypePatternMatching() {
    compareFunction("handle",
        "(handle: (()= req) -> (\n" +
            "  => ([::] HttpRequest name <- (. params name)) \n" +
            "    | (comput (. name) (== (. 'Dhanji'))) : (comput (. 'Hi')) \n" +
            "    | (comput (. name) (== (. 'Dude'))) : (comput (. 'Bye'))) \n" +
            "  => wildcard : (comput (. Nothing)))",
        "handle(req) =>\n" +
            "  HttpRequest[name <- params.name]   | name == 'Dhanji'  : 'Hi'\n" +
            "                                     | name == 'Dude'    : 'Bye'\n" +
            "  *                                                      : Nothing\n");
  }

  @Test
  public final void guardedObjectTypePatternMatchingWithOtherwise() {
    compareFunction("handle",
        "(handle: (()= req) -> (\n" +
            "  => ([::] HttpRequest name <- (. params name)) \n" +
            "    | (comput (. name) (== (. 'Dhanji'))) : (comput (. 'Hi')) \n" +
            "    | (comput (. name) (== (. 'Dude'))) : (comput (. 'Bye')) \n" +
            "    | otherwise : (comput (. 'Error'))) \n" +
            "  => wildcard : (comput (. Nothing)))",
        "handle(req) =>\n" +
            "  HttpRequest[name <- params.name]   | name == 'Dhanji'  : 'Hi'\n" +
            "                                     | name == 'Dude'    : 'Bye'\n" +
            "                                     | else              : 'Error'\n" +
            "  *                                                      : Nothing\n");
  }
}
