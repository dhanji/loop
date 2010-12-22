package loop.type;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for working with types.
 */
public class Types {
  public static final Type STRING = new Type("String");
  public static final Type INTEGER = new Type("Integer", "int", "0");
  public static final Type VOID = new Type("Void", "void", "null");

  
  public static final Type LIST = new Type("List", "List", "Lists.of()");
  public static final Type MAP = new Type("Map", "Map", "Maps.of()");

  /**
   * Primitive to Java boxed type.
   */
  private static final Map<Type, String> PRIMITIVES = new HashMap<Type, String>();
  static {
    PRIMITIVES.put(INTEGER, "Integer");
  }

  public static boolean isPrimitive(Type keyType) {
    return PRIMITIVES.containsKey(keyType);
  }

  public static String boxedTypeOf(Type t) {
    String boxed = PRIMITIVES.get(t);

    return boxed == null ? t.javaType() : boxed;
  }
}
