package loop.ast;

/**
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class PrivateField extends Node {
  private final String name;

  public PrivateField(String name) {
    this.name = name.substring(1);
  }

  public String name() {
    return '@' + name;
  }

  @Override
  public String toSymbol() {
    return "@" + name;
  }

  @Override
  public String toString() {
    return "PrivateField{" +
        "name='" + name + '\'' +
        '}';
  }
}
