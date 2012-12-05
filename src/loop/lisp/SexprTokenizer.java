package loop.lisp;

import loop.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dhanji R. Prasanna
 */
public class SexprTokenizer {
  private final String input;

  public SexprTokenizer(String input) {
    this.input = input;
  }

  private static final int NON = 0; // MUST be zero
  private static final int SINGLE_TOKEN = 1;

  private static final int[] DELIMITERS = new int[256];
  private static final boolean[] STRING_TERMINATORS = new boolean[256];

  static {
    // SINGLE token delimiters are one char in length in any context
    DELIMITERS['.'] = SINGLE_TOKEN;
    DELIMITERS[','] = SINGLE_TOKEN;
    DELIMITERS[';'] = SINGLE_TOKEN;
    DELIMITERS['('] = SINGLE_TOKEN;
    DELIMITERS[')'] = SINGLE_TOKEN;
    DELIMITERS['['] = SINGLE_TOKEN;
    DELIMITERS[']'] = SINGLE_TOKEN;
    DELIMITERS['{'] = SINGLE_TOKEN;
    DELIMITERS['}'] = SINGLE_TOKEN;

    STRING_TERMINATORS['"'] = true;
    STRING_TERMINATORS['\''] = true;
    STRING_TERMINATORS['`'] = true;
  }

  public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<Token>();
    char[] input = this.input.toCharArray();

    int line = 0, column = 0;

    int i = 0, start = 0;
    boolean inWhitespace = false, inDelimiter = false, inComment = false;
    char inStringSequence = 0;
    for (; i < input.length; i++) {
      char c = input[i];
      column++;

      if (c == '\n') {
        line++;
        column = 0;
      }

      // strings and sequences
      if (STRING_TERMINATORS[c] && !inComment) {

        if (inStringSequence > 0) {

          // end of the current string sequence. bake.
          if (inStringSequence == c) {
            // +1 to include the terminating token.
            bakeToken(tokens, input, i + 1, start, line, column);
            start = i + 1;

            inStringSequence = 0; // reset to normal language
            continue;
          }
          // it's a string terminator but it's ok, it's part of the string, ignore...

        } else {
          // Also bake if there is any leading tokenage.
          if (i > start) {
            bakeToken(tokens, input, i, start, line, column);
            start = i;
          }

          inStringSequence = c; // start string
        }
      }

      // skip everything if we're in a string...
      if (inStringSequence > 0)
        continue;

      // Comments beginning with #
      if (c == ';' || c == '#') {
        inComment = true;
      }

      // We run the comment until the end of the line
      if (inComment) {
        if (c == '\n')
          inComment = false;

        start = i;
        continue;
      }

      // whitespace is ignored unless it is leading...
      if (Character.isWhitespace(c)) {
        inDelimiter = false;

        if (!inWhitespace) {
          //bake token
          bakeToken(tokens, input, i, start, line, column);
          inWhitespace = true;
        }

        // skip whitespace
        start = i + 1;
        continue;
      }

      // any non-whitespace character encountered
      inWhitespace = false;

      // For delimiters that are 1-char long in all contexts,
      // break early.
      if (isSingleTokenDelimiter(c)) {

        bakeToken(tokens, input, i, start, line, column);
        start = i;

        // Also add the delimiter.
        bakeToken(tokens, input, i + 1, start, line, column);
        start = i + 1;
        continue;
      }

      // is delimiter
      if (isDelimiter(c)) {

        if (!inDelimiter) {
          bakeToken(tokens, input, i, start, line, column);
          inDelimiter = true;
          start = i;
        }

        continue;
      }

      // if coming out of a delimiter, we still need to bake
      if (inDelimiter) {
        bakeToken(tokens, input, i, start, line, column);
        start = i;
        inDelimiter = false;
      }
    }

    // collect residual token
    if (i > start && !inComment) {
      // we don't want trailing whitespace
      bakeToken(tokens, input, i, start, line, column);
    }

    return tokens;
  }


  static boolean isSingleTokenDelimiter(char c) {
    return DELIMITERS[c] == SINGLE_TOKEN;
  }

  private static boolean isDelimiter(char c) {
    return DELIMITERS[c] != NON;
  }

  private static void bakeToken(List<Token> tokens,
                                char[] input,
                                int i,
                                int start,
                                int line,
                                int column) {
    if (i > start) {
      String value = new String(input, start, i - start);

      // Hack to elide whitespace that sneaks in due to the leading whitespace detector.
      if (value.trim().isEmpty())
        return;

      // remove this disgusting hack when you can fix the lexer.
      tokens.add(new Token(value, Token.Kind.determine(value), line, column));
    }
  }
}
