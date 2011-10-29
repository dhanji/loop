package loop;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class Context {
  String name;
  final List<String> arguments = new ArrayList<String>();

  public Context(String name) {
    this.name = name;
  }
}
