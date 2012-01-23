package loop;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopSyntaxException extends RuntimeException {
  private final List<ParseError> errors;

  public LoopSyntaxException(String message, List<ParseError> errors) {
    super(message);
    this.errors = errors;
  }

  public LoopSyntaxException(String message) {
    super(message);

    this.errors = null;
  }

  public List<ParseError> getErrors() {
    return errors;
  }
}
