package loop.ast.script;

import loop.ast.Node;

import java.util.List;

/**
 * Import declaration at the top of a script.
 */
public class RequireDecl extends Node {
  public final List<String> moduleChain;
  public final String javaLiteral;
  public final String alias;

  public RequireDecl(List<String> moduleChain, String alias) {
    this.moduleChain = moduleChain;
    this.javaLiteral = null;
    this.alias = alias;
  }

  public RequireDecl(String javaLiteral) {
    this(javaLiteral, null);
  }

  public RequireDecl(String javaLiteral, String alias) {
    this.javaLiteral = javaLiteral.substring(1, javaLiteral.length() - 1);
    this.moduleChain = null;
    this.alias = alias;
  }

  @Override
  public String toSymbol() {
    return "require " + (moduleChain == null ? '`' + javaLiteral + '`' : moduleChain)
        + (alias != null ? " as " + alias : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RequireDecl that = (RequireDecl) o;

    if (javaLiteral != null ? !javaLiteral.equals(that.javaLiteral) : that.javaLiteral != null)
      return false;
    if (moduleChain != null ? !moduleChain.equals(that.moduleChain) : that.moduleChain != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = moduleChain != null ? moduleChain.hashCode() : 0;
    result = 31 * result + (javaLiteral != null ? javaLiteral.hashCode() : 0);
    return result;
  }
}
