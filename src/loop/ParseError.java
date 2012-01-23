package loop;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ParseError {
  private final String message;
  private final Token token;

  public ParseError(String message, Token token) {
    this.message = message;
    this.token = token;
  }

  public String getMessage() {
    return message;
  }

  public int line() {
    return token.line;
  }

  public int column() {
    return token.column;
  }
}
