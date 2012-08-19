package loop.ast;

/**
 * Any kind of member access, a function call or a field/property dereference.
 */
public abstract class MemberAccess extends Node {
  public abstract Node postfix(boolean postfix);

  public abstract MemberAccess javaStatic(boolean javaStatic);

  public abstract boolean isJavaStatic();

  public abstract String name();
}
