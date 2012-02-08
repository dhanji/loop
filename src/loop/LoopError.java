package loop;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopError {
  private final String message;
  public LoopError(Exception cause) {
    this.message = cause.getClass().getName() + "(" + cause.getMessage() + ")";
  }

  public LoopError(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
  return "#error" + (message != null ? ": " + message : "");
  }
}
