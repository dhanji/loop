package loop;

/**
 * @author Dhanji R. Prasanna
 */
public class StringToken {
  public final String value;
  public final Kind kind;

  public final int line;
  public final int column;

  public StringToken(String value, Kind kind, int line, int column) {
    this.value = value;
    this.kind = kind;
    this.line = line;
    this.column = column;
  }

  public static enum Kind {
    CHAR_SEQUENCE,
    EXPRESSION
  }

  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (kind != null ? kind.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "StringToken{" +
        "value='" + value + '\'' +
        ", kind=" + kind +
        '}';
  }
}
