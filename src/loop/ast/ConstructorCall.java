package loop.ast;

import loop.Parser;

/**
 * Represents a constructor call on either Java or Loop types.
 */
public class ConstructorCall extends Node {
  public final String modulePart;
  public final String name;

  private CallArguments args;

  public ConstructorCall(String modulePart, String name, CallArguments args) {
    this.modulePart = modulePart;
    this.name = name;
    this.args = args;
  }


  public CallArguments args() {
    return args;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return "ConstructorCall{" + name + args.toString() + "}";
  }

  @Override
  public String toSymbol() {
    return "new " + (modulePart != null ? modulePart : "") + name + Parser.stringify(args);
  }
}
