package loop.ast;

import loop.Parser;

/**
 * Represents a method call or member dereference.
 */
public class Call extends Node {
  public final String name;
  public final boolean isFunction;

  private CallArguments args;
  private static final String PRINT = "System.out.println";

  public Call(String name, boolean function, CallArguments args) {
    // HACK! rewrites print calls to Java sout
    this.name = "print".equals(name) ? PRINT : name;
    this.isFunction = function;
    this.args = args;
  }

  public CallArguments args() {
    return args;
  }

  @Override
  public String toString() {
    return "Call{" + name + (isFunction ? args.toString() : "") + "}";
  }

  @Override
  public String toSymbol() {
    return name + (isFunction ? Parser.stringify(args) : "");
  }
}
