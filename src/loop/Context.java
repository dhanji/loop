package loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class Context {
  String name;
  final List<String> arguments = new ArrayList<String>();
  final Map<String, Integer> argumentIndex = new HashMap<String, Integer>();

  public Context(String name) {
    this.name = name;
  }
}
