package loop.compile;

import java.util.Collection;

/**
 * Various utilities for working with local variables.
 */
public class Variables {
  public static String declare(Collection<LocalVar> variables) {
    StringBuilder builder = new StringBuilder();
    for (LocalVar variable : variables) {
      builder.append(variable.getType().javaType());
      builder.append(" ");
      builder.append(variable.getName());
      builder.append(" = ");
      builder.append(variable.getValue());
      builder.append(";\n");
    }

    return builder.toString();
  }
}
