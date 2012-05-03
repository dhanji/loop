package loop.ast;

/**
 * A Type literal. Similar to XX.class in Java.
 */
public class TypeLiteral extends Node {
  public static final String NOTHING = "Nothing";
  public final String name;

  public TypeLiteral(String value) {
    name = value;
  }

  @Override
  public String toString() {
    return "TypeLiteral{" +
        "name='" + name + '\'' +
        '}';
  }

  @Override
  public String toSymbol() {
    return name;
  }
}
