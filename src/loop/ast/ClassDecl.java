package loop.ast;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClassDecl extends Node {
  public final String name;
  public final boolean immutable;

  public ClassDecl(String name, boolean immutable) {
    this.name = name;
    this.immutable = immutable;
  }

  @Override public String toSymbol() {
    return (immutable ? "immutable_" : "") + "class " + name + " ->";
  }
}
