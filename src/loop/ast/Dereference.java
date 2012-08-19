package loop.ast;

/**
 * Represents a method call or member dereference.
 */
public class Dereference extends MemberAccess {
  private final String name;

  private boolean javaStatic;
  private boolean constant;
  private String namespace;
  private boolean postfix;

  public Dereference(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  public MemberAccess javaStatic(boolean isStatic) {
    this.javaStatic = isStatic;

    return this;
  }

  public String namespace() {
    return namespace;
  }

  public void namespace(String namespace) {
    this.namespace = namespace;
  }

  public boolean isJavaStatic() {
    return javaStatic;
  }

  public boolean constant() {
    return constant;
  }

  public Dereference constant(boolean constant) {
    this.constant = constant;

    return this;
  }

  @Override
  public String toString() {
    return "Dereference{" + name + "}";
  }

  @Override
  public String toSymbol() {
    return name;
  }

  @Override
  public Node postfix(boolean postfix) {
    this.postfix = postfix;

    return this;
  }
}
