package loop.ast;

import loop.Parser;
import loop.StringLerpTokenizer;
import loop.StringToken;
import loop.Token;
import loop.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StringLiteral extends Node {
  public final String value;
  public final List<Node> parts;

  public StringLiteral(String value) {
    this.value = value;

    // Single quote strings cannot be lerped.
    if (value.charAt(0) == '\'') {
      parts = null;
      return;
    }

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

  public StringLiteral(String value, List<Node> parts) {
    this.value = value;
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
