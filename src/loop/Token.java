package loop;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dhanji R. Prasanna
 */
public class Token {
  public final String value;
  public final Kind kind;

  public Token(String value, Kind kind) {
    this.value = value;
    this.kind = kind;
  }

  public static enum Kind {
    PRIVATE_FIELD,
    IDENT,
    TYPE_IDENT,
    INTEGER,
    STRING,
    REGEX,
    DOT,

    PLUS,
    MINUS,
    DIVIDE,
    STAR,
    MODULUS,

    COMMA,

    ASSIGN,
    ARROW,
    HASHROCKET,

    // Comparison operators
    EQUALS,
    LESSER,
    GREATER,
    LEQ,
    GEQ,

    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    LBRACKET,
    RBRACKET,

    // keywords
    REQUIRE,
    MODULE,

    CLASS,
    CONSTRUCTOR,
    OR,
    AND,
    NOT,
    FOR,
    IN,
    IF,
    THEN,
    ELSE,
    UNLESS,
    WHERE,

    WHEN,
    SWITCH,

    // specials
    EOL,
    INDENT;

    private static final Map<String, Kind> TOKEN_MAP = new HashMap<String, Kind>();

    static {
      // can we optimize with chars?
      TOKEN_MAP.put("=", ASSIGN);
      TOKEN_MAP.put(".", DOT);
      TOKEN_MAP.put("+", PLUS);
      TOKEN_MAP.put("-", MINUS);
      TOKEN_MAP.put("/", DIVIDE);
      TOKEN_MAP.put("*", STAR);
      TOKEN_MAP.put("%", MODULUS);
      TOKEN_MAP.put(":", ASSIGN);
      TOKEN_MAP.put(",", COMMA);
      TOKEN_MAP.put("->", ARROW);

      TOKEN_MAP.put("==", EQUALS);
      TOKEN_MAP.put("<=", LEQ);
      TOKEN_MAP.put(">=", GEQ);
      TOKEN_MAP.put("<", LESSER);
      TOKEN_MAP.put(">", GREATER);
      TOKEN_MAP.put("=>", HASHROCKET);

      TOKEN_MAP.put("(", LPAREN);
      TOKEN_MAP.put(")", RPAREN);
      TOKEN_MAP.put("{", LBRACE);
      TOKEN_MAP.put("}", RBRACE);
      TOKEN_MAP.put("[", LBRACKET);
      TOKEN_MAP.put("]", RBRACKET);
      TOKEN_MAP.put("\n", EOL);

      TOKEN_MAP.put("if", IF);
      TOKEN_MAP.put("then", THEN);
      TOKEN_MAP.put("else", ELSE);
      TOKEN_MAP.put("when", WHEN);
      TOKEN_MAP.put("unless", UNLESS);
      TOKEN_MAP.put("where", WHERE);
      TOKEN_MAP.put("for", FOR);
      TOKEN_MAP.put("in", IN);
      TOKEN_MAP.put("constructor", CONSTRUCTOR);
      TOKEN_MAP.put("class", CLASS);

      TOKEN_MAP.put("require", REQUIRE);
      TOKEN_MAP.put("module", MODULE);


      TOKEN_MAP.put("||", OR);
      TOKEN_MAP.put("or", OR);
      TOKEN_MAP.put("&&", AND);
      TOKEN_MAP.put("and", AND);
      TOKEN_MAP.put("!", NOT);
      TOKEN_MAP.put("not", NOT);
    }

    /**
     * from token text, determines kind.
     */
    public static Kind determine(String value) {
      char first = value.charAt(0);
      if (first == '@')
        return PRIVATE_FIELD;

      if (first == '"' || first == '\'')
        return STRING;

      Kind knownKind = TOKEN_MAP.get(value);
      if (null != knownKind)
        return knownKind;

      // Integers (can this be more efficient?)
      if (value.matches("[0-9]+")) {
        return INTEGER;
      }

      if (Character.isUpperCase(first)) {
        return TYPE_IDENT;
      }
      return IDENT;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Token token = (Token) o;

    return (kind == token.kind)
        && !(value != null ? !value.equals(token.value) : token.value != null);

  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Token{" +
        "value='" + value + '\'' +
        ", kind=" + kind +
        '}';
  }
}
