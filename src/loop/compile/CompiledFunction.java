package loop.compile;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * Represents a function that has been type-checked, compiled
 * and emitted as Java source code.
*/
class CompiledFunction {
  public final String signature;
  public final String body;

  private CtMethod method;

  CompiledFunction(String signature, String body) {
    this.signature = signature;
    this.body = body;
  }

  @Override
  public String toString() {
    return signature + "\n" + body;
  }

  public CtMethod toJavassistMethod(CtClass clazz) throws CannotCompileException {
    return method = CtNewMethod.make(signature, clazz);
  }

  public void populate() throws CannotCompileException {
    method.setBody(body);
  }
}
