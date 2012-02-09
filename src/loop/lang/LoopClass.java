package loop.lang;

import java.util.Map;

/**
 * Represents a type in loop.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopClass {
  public final String name;

  public LoopClass(String name) { this.name = name; }

  public static LoopObject newInstance(String type, Map<Object, Object> startup) {
    LoopObject object = new LoopObject(new LoopClass(type));
    if (startup != null)
      object.putAll(startup);

    return object;
  }
}
