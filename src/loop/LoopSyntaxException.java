package loop;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopSyntaxException extends RuntimeException {
  private final Executable executable;

  public LoopSyntaxException(String message, Executable executable) {
    super(message);

    this.executable = executable;
  }

  public LoopSyntaxException() {
    super("Syntax errors exist");
    this.executable = null;
  }

  public List<ParseError> getErrors() {
    return executable.getParseErrors();
  }
}
