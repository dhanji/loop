package loop;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class StaticError implements AnnotatedError {
  private final String message;
  private final int line;
  private final int column;

  public StaticError(String message, Token token) {
    this.message = message;
    this.line = token.line;
    this.column = token.column;
  }

  public StaticError(String message, int line, int column) {
    this.message = message;
    this.line = line;
    this.column = column;
  }

  public String getMessage() {
    return message;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }
}
