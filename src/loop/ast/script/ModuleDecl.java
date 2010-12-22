package loop.ast.script;

import loop.ast.Node;

import java.util.List;

/**
 * Import declaration at the top of a script.
 */
public class ModuleDecl extends Node {
  private final List<String> moduleChain;

  public ModuleDecl(List<String> moduleChain) {
    this.moduleChain = moduleChain;
  }

  @Override
  public String toSymbol() {
    return "module " + moduleChain;
  }
}
