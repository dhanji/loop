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
    System.out.println(Tokenizer.detokenize(new Tokenizer(        "reverse(list) =>\n" +
            "  []          : []\n" +
            "  [x:xs]      : reverse(xs) + x").tokenize()));
    compareFunction("reverse",
        "(reverse: (()= list) -> \n" +
        "  => [] : (comput list))",
        "reverse(list) =>\n" +
            "  []          : []\n"
//            + "  [x:xs]      : reverse(xs) + x"
    );

  }
}
