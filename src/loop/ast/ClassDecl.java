package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClassDecl extends Node {
  public final String name;

  public ClassDecl(String name) {
    this.name = name;
  }

  @Override public String toSymbol() {
    return "class " + name + " ->";
  }
}
