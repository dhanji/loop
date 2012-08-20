package loop;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dhanji R. Prasanna
 */
public class Token {
  public final String value;
  public final Kind kind;

  public final int line;
  public final int column;

  public Token(String value, Kind kind, int line, int column) {
    this.value = kind == Kind.IDENT ? value.trim() : value;
    this.kind = kind;
    this.line = line;
    this.column = column;
  }

  public static enum Kind {
    PRIVATE_FIELD,
    ANONYMOUS_TOKEN,
    IDENT,
    TYPE_IDENT,
    BIG_INTEGER,
    INTEGER,
    FLOAT,
    LONG,
    STRING,
    JAVA_LITERAL,
    REGEX,
    DOT,

    PLUS,
    MINUS,
    DIVIDE,
    STAR,
    MODULUS,

    COMMA,
    PIPE,

    ASSIGN,
    ARROW,
    UNARROW,
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

    IMMUTABLE,
    CLASS,
    NEW,
    OR,
    AND,
    NOT,
    FOR,
    IN,
    IF,
    UNLESS,
    THEN,
    ELSE,
    WHERE,

    TRUE,
    FALSE,

    WHEN,

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
      TOKEN_MAP.put("|", PIPE);
      TOKEN_MAP.put(",", COMMA);
      TOKEN_MAP.put("->", ARROW);
      TOKEN_MAP.put("<-", UNARROW);

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
      TOKEN_MAP.put("unless", UNLESS);
      TOKEN_MAP.put("then", THEN);
      TOKEN_MAP.put("else", ELSE);
      TOKEN_MAP.put("when", WHEN);
      TOKEN_MAP.put("where", WHERE);
      TOKEN_MAP.put("for", FOR);
      TOKEN_MAP.put("in", IN);
      TOKEN_MAP.put("new", NEW);
      TOKEN_MAP.put("class", CLASS);
      TOKEN_MAP.put("immutable", IMMUTABLE);

      TOKEN_MAP.put("require", REQUIRE);
      TOKEN_MAP.put("module", MODULE);


      TOKEN_MAP.put("or", OR);
      TOKEN_MAP.put("and", AND);
      TOKEN_MAP.put("not", NOT);


      TOKEN_MAP.put("true", TRUE);
      TOKEN_MAP.put("false", FALSE);
    }

    /**
     * from token text, determines kind.
     */
    public static Kind determine(String value) {
      char first = value.charAt(0);

      if (value.matches("@[0-9]+"))
        return BIG_INTEGER;
      else if (first == '@') {
        return value.length() > 1 ? PRIVATE_FIELD : ANONYMOUS_TOKEN;
      }

      if (first == '"' || first == '\'')
        return STRING;
      else if (first == '`' && value.endsWith("`"))
        return JAVA_LITERAL;

      Kind knownKind = TOKEN_MAP.get(value);
      if (null != knownKind)
        return knownKind;

      // Integers (can this be more efficient?)
      if (value.matches("[0-9]+"))
        return INTEGER;
      else if (value.matches("[0-9]+F"))
        return FLOAT;
      else if (value.matches("[0-9]+L"))
        return LONG;

      if (Character.isUpperCase(first)) {
        return TYPE_IDENT;
      }

      if (value.endsWith("/"))
        return REGEX;

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
