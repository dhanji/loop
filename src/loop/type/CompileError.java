package loop.type;

/**
 * Represents a compilation or type error.
 */
class CompileError {
  private final String message;

  public CompileError(String message) {
    this.message = message;
  }

  String message() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CompileError that = (CompileError) o;

    if (message != null ? !message.equals(that.message) : that.message != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return message != null ? message.hashCode() : 0;
  }
}
