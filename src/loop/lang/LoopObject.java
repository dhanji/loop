package loop.lang;

import java.util.HashMap;

/**
 * The root object of all object instances in loop. Actually this is the
 * Java class that backs all instances of all loop types.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopObject extends HashMap<Object, Object> {
  private final LoopClass type;

  public LoopObject(LoopClass type) { this.type = type; }

  public LoopClass getType() {
    return type;
  }
}
