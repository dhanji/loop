package loop;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes strings that require interpolation. E.g.:
 * <p/>
 * "Hello, @{name}!"
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StringLerpTokenizer {
  public static List<StringToken> tokenize(String source) {
    // Strip quotes.
    source = source.substring(1, source.length() - 1);

    int braces = 0;
    int length = source.length();

    List<StringToken> tokens = new ArrayList<StringToken>();

    StringBuilder currentToken = new StringBuilder();
    for (int i = 0; i < length; i++) {
      char c = source.charAt(i);

      if (c == '@') {
        if (i < length - 1 && source.charAt(i + 1) == '{') {
          braces += 1;

          tokens.add(new StringToken(currentToken.toString(), StringToken.Kind.CHAR_SEQUENCE, 0, 0));
          currentToken = new StringBuilder();
        }
      } else if (c == '}') {
        braces--;

        if (braces == 0) {
          // Delete leading '@{'
          currentToken.deleteCharAt(0).deleteCharAt(0);

          // bake token!
          tokens.add(new StringToken(currentToken.toString(), StringToken.Kind.EXPRESSION, 0, 0));
          currentToken = new StringBuilder();

          // Skip trailing '}'
          continue;
        }
      }

      currentToken.append(c);
    }

    // Slurp up remainder.
    if (currentToken.length() > 0)
      tokens.add(new StringToken(currentToken.toString(), StringToken.Kind.CHAR_SEQUENCE, 0, 0));

    return tokens;
  }
}
