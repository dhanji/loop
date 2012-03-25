package loop;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopCompileException extends RuntimeException {
  private final Executable executable;

  public LoopCompileException(String message, Executable executable) {
    super(message);

    this.executable = executable;
  }

  public LoopCompileException() {
    super("Syntax errors exist");
    this.executable = null;
  }

  public List<AnnotatedError> getErrors() {
    return executable.getStaticErrors();
  }
}
