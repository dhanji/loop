package loop.compile;

import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.type.Errors;
import loop.type.Types;

/**
 * A lexical scope meant specifically for use at the module level (akin to
 * packages in Java).
 */
public class ModuleScope extends BasicScope {
  public ModuleScope(Errors errors) {
    super(errors, null);

    init();
  }

  private void init() {
    // These types are visible in every module.
    load(Types.INTEGER);
    load(Types.STRING);
    load(Types.VOID);

    load(Types.LIST);
    load(Types.MAP);

    ArgDeclList args = new ArgDeclList();
    args.add(new ArgDeclList.Argument("arg1", "Integer"));
    load(new FunctionDecl("print", args));
  }
}
