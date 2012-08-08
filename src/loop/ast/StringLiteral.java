package loop.ast;

import loop.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StringLiteral extends Node {
  public static final String NEWLINE_ESCAPES = "[^\\\\]\\\\n";
  public final String value;
  public final List<Node> parts;

  public StringLiteral(String value) {
    // Single quote strings cannot be lerped.
    if (value.charAt(0) == '\'') {
      parts = null;
      this.value = value.replaceAll(NEWLINE_ESCAPES, "\n");
      return;
    }

    this.value = value;

    // Parse any expressions embedded in this string.
    List<StringToken> stringTokens = StringLerpTokenizer.tokenize(value);
    parts = new ArrayList<Node>(stringTokens.size());
    for (StringToken stringToken : stringTokens) {
      if (stringToken.kind == StringToken.Kind.EXPRESSION) {
        List<Token> tokens = new Tokenizer(stringToken.value).tokenize();

        parts.add(new Parser(tokens).computation());
      } else
        parts.add(new StringLiteral(stringToken.value, null));
    }
  }

  private StringLiteral(String value, List<Node> parts) {
    this.value = Escaper.unescape_perl_string(value);
    this.parts = parts;
  }

  public String unquotedValue() {
    return value != null ? value.substring(1, value.length() - 1) : null; // strip quotes
  }

  @Override
  public String toSymbol() {
    return value;
  }

  @Override
  public String toString() {
    return "String{" +
        "'" + value + '\'' +
        '}';
  }
}
