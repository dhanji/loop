package loop.ast;

import loop.Parser;

/**
 * An argument list to a function. May be positional or named.
 *
 * example:
 * (x, y, z)
 *
 * or:
 *
 * (arg0: x, arg1: x + 1, arg2: [1..3])
 */
public class CallArguments extends Node {
  private final boolean positional;

  public CallArguments(boolean positional) {
    this.positional = positional;
  }

  public boolean isPositional() {
    return positional;
  }

  public static class NamedArg extends Node {
    public final String name;
    public final Node arg;

    public NamedArg(String name, Node arg) {
      this.name = name;
      this.arg = arg;
    }

    @Override
    public String toSymbol() {
      return name + ": " + Parser.stringify(arg);
    }
  }

  @Override
  public String toSymbol() {
    return children().isEmpty() ? "()" : "()=";
  }
}
