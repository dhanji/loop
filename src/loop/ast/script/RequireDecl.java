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
    this.javaLiteral = javaLiteral.substring(1, javaLiteral.length() - 1);
    this.moduleChain = null;
    this.alias = null;
  }

  @Override
  public String toSymbol() {
    return "require " + (moduleChain == null ? '`' + javaLiteral + '`' : moduleChain)
        + (alias != null ? " as " + alias : "");
  }
}
