package loop.type;

/**
 * A type in the loop language.
 */
public class Type {
  private final String name;
  private final String javaType; // Equivalent Java type (if any)
  private final String defaultValue;

  public Type(String name) {
    this.name = name;
    this.javaType = name;
    this.defaultValue = "null";
  }

  public Type(String name, String javaType, String defaultValue) {
    this.name = name;
    this.javaType = javaType;
    this.defaultValue = defaultValue;
  }

  public String name() {
    return name;
  }

  public String javaType() {
    return javaType;
  }

  public String defaultValue() {
    return defaultValue;
  }

  @Override
  public String toString() {
    return javaType;
  }
}
