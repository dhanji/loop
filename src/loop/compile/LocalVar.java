package loop.compile;

import loop.type.Type;

/**
 * Value object representing the declaration of a local variable
 * and some metadata about it.
 */
public class LocalVar {
  private final String name;
  private final Type type;
  private final boolean isArgument;
  private final String altName;

  private String initialValue;

  LocalVar(String name, Type type) {
    this.name = name;
    this.altName = name;
    this.type = type;
    this.isArgument = false;
  }
  
  LocalVar(String name, Type type, String altName) {
    this.name = name;
    this.type = type;
    this.isArgument = true;
    this.altName = altName;
  }

  public String getName() {
    return altName;
  }

  public Type getType() {
    return type;
  }

  public boolean isArgument() {
    return isArgument;
  }

  public String getValue() {
    if (null == initialValue) {
      return type.defaultValue();
    }

    return initialValue;
  }
}
