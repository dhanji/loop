package loop.compile;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.Modifier;
import loop.LoopCompiler;
import loop.ast.Node;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.type.Errors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jade compiler. Takes a reduced Jade AST, runs type analysis over it and then
 * emits it as Java source code, which is then compiled to Java classes.
 */
public class LoopJavassistCompiler implements LoopCompiler {
  private final Unit compilationUnit;
  private final String enclosingTypeName;

  private final Errors errors = new Errors();
  private final ClassPool pool = ClassPool.getDefault();

  private CtClass clazz;
  private String indent = "";
  private Map<String, Node> declarations = new HashMap<String, Node>();
  private StringBuilder declarationsEmitted = new StringBuilder();

  private StringBuilder out;
  private int marker;

  private Scope currentScope;

  public LoopJavassistCompiler(String enclosingTypeName, Unit compilationUnit) {
    this.compilationUnit = compilationUnit;
    this.enclosingTypeName = enclosingTypeName;

    pool.importPackage("java.util");
    pool.importPackage("loop.runtime");
  }

  public Class<?> compile() {
    clazz = pool.makeClass(enclosingTypeName);
    currentScope = new ModuleScope(errors); // module-level scope comes with some predefined types.
    
    try {
      compileFunctions();

      return clazz.toClass();
    } catch (CannotCompileException e) {
      throw new RuntimeException(e);
    }
  }

  // emits text at the current indentation level.
  private void writeIndented(String st) {
    out.append(indent);
    out.append(st);
  }

  public void write(String st) {
    out.append(st);
  }

  public void mark() {
    this.marker = out.length();
  }

  public void writeAtMarker(String st) {
    out.insert(marker, st);
  }


  public Scope currentScope() {
    return currentScope;
  }

  public Errors errors() {
    return errors;
  }

  public void write(int value) {
    out.append(value);
  }

  private void indent() {
    indent += "  ";
  }

  private void outdent() {
    if (indent.length() >= 2) {
      indent = indent.substring(0, indent.length() - 2);
    }
  }

  private void compileFunctions() throws CannotCompileException {
    // Maybe replace this with a templating system like StringTemplate or MVEL.

    // Step 0: Attempt to infer types of polymorphic functions if possible. This may
    // fail for some functions. But the idea is if we can cleverly quantify a function
    // over a concrete type bound from its body alone, then we should do that to
    // improve the type analysis.
    for (FunctionDecl function : compilationUnit.functions()) {
      if (isMain(function))
        continue;

      BasicScope scope = new BasicScope(errors, currentScope);

      // Load this temporary scope with the unbound argument names.
      for (Node node : function.arguments().children()) {
        ArgDeclList.Argument arg = (ArgDeclList.Argument)node;

//        scope.declareArgument(arg.name(), Types.arg.type());
      }

      function.attemptInferType(scope);
    }

    // Step 1: Go through and compile all concrete functions. This triggers code paths through
    // polymorphic functions, binding them to types and thus making them concrete.
    // Then in a second pass we compile these newly discovered concrete functions.
    List<CompiledFunction> compiledFunctions = new ArrayList<CompiledFunction>();
    for (FunctionDecl function : compilationUnit.functions()) {

      // Main should always be emitted as we know its type and there should
      // only ever be one concrete instance of it.
      if (isMain(function)) {
        // Infer main anyway, this helps trigger overload resolution for any called
        // polymorphic functions.
        new FunctionCompiler(errors, currentScope).compileConcreteFunction(function);
        continue;
      }

      // Don't bother inferring types for functions that are polymorphic. They will
      // get witnessed and resolved from concrete call paths.
      if (function.isPolymorphic())
        compilePolymorphicFunction(function);
      else
        compiledFunctions.add(new FunctionCompiler(errors, currentScope)
            .compileConcreteFunction(function));
    }

    // Step 2: Compile witnessed overloads of encountered polymorphic functions.
    for (Scope.Witness witness : currentScope.getWitnesses()) {
      new FunctionCompiler(errors, currentScope).compileConcreteFunction(witness.functionDecl);
    }

    // Step 3: Now Java-compile all the functions in one go for this module.
    System.out.println(compiledFunctions);
    
    if (errors.hasErrors()) {
      throw new RuntimeException(errors.toString());
    }

    // We need to first declare all the functions with abstract signatures.
    for (CompiledFunction compiledFunction : compiledFunctions) {
      try {
        clazz.addMethod(compiledFunction.toJavassistMethod(clazz));
      } catch (CannotCompileException e) {
        errors.exception(e);
      }
    }

    // Then fill in function bodies for each abstract method.
    for (CompiledFunction compiledFunction : compiledFunctions) {
      compiledFunction.populate();
    }

    // Finally the class must be made concrete again (adding abstract methods
    // automatically turns the class abstract).
    clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT);
  }


  private void compilePolymorphicFunction(FunctionDecl func) {
    // Load this newly found function into the containing scope for (import it).
    currentScope.load(func);
  }


  private static boolean isMain(FunctionDecl function) {
    return "main".equals(function.name());
  }

}
