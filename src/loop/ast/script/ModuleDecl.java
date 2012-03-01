package loop.ast.script;

import loop.ast.Node;

import java.util.Arrays;
import java.util.List;

/**
 * Import declaration at the top of a script.
 */
public class ModuleDecl extends Node {
  public static final ModuleDecl DEFAULT = new ModuleDecl(Arrays.asList("_default"));

  public final List<String> moduleChain;

  public ModuleDecl(List<String> moduleChain) {
    this.moduleChain = moduleChain;
  }

  @Override
  public String toSymbol() {
    return "module " + moduleChain;
  }
}
