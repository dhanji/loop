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
  private boolean javaStatic;
  private boolean postfix = false;

  public Call(String name, boolean function, CallArguments args) {
    // HACK! rewrites print calls to Java sout
    this.name = "print".equals(name) ? PRINT : name;
    this.isFunction = function;
    this.args = args;
  }

  public CallArguments args() {
    return args;
  }

  public String name() {
    return name;
  }

  public void javaStatic(boolean javaStatic) {
    this.javaStatic = javaStatic;
  }

  public void postfix(boolean postfix) {
    this.postfix = postfix;
  }

  public boolean isPostfix() {
    return postfix;
  }

  public boolean isJavaStatic() {
    return javaStatic;
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
