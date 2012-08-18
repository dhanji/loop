package loop.ast;

import loop.Parser;

/**
 * Represents a method call or member dereference.
 */
public class Call extends Node {
  public final String name;
  public final boolean isFunction;

  private CallArguments args;
  private boolean javaStatic;
  private boolean postfix;
  private boolean callJava;
  private boolean tailCall;
  private String namespace;

  public Call(String name, boolean function, CallArguments args) {
    this.name = name;
    this.isFunction = function;
    this.args = args;
  }

  public CallArguments args() {
    return args;
  }

  public String name() {
    return name;
  }

  public void tailCall(boolean isTailCall) {
    this.tailCall = isTailCall;
  }

  public boolean isTailCall() {
    return tailCall;
  }

  public Call javaStatic(boolean javaStatic) {
    this.javaStatic = javaStatic;

    return this;
  }

  public String namespace() {
    return namespace;
  }

  public void namespace(String namespace) {
    this.namespace = namespace;
  }

  public Call postfix(boolean postfix) {
    this.postfix = postfix;

    return this;
  }

  public boolean isPostfix() {
    return postfix;
  }

  public boolean isJavaStatic() {
    return javaStatic;
  }

  public boolean callJava() {
    return callJava;
  }

  public Call callJava(boolean callJava) {
    this.callJava = callJava;

    return this;
  }

  @Override
  public String toString() {
    return "Call{" + name + " " + (isFunction ? args.toString() : "") + "}";
  }

  @Override
  public String toSymbol() {
    return name + (isFunction ? Parser.stringify(args) : "");
  }
}
