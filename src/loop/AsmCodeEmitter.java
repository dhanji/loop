package loop;

import loop.ast.*;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.runtime.Closure;
import loop.runtime.Scope;
import loop.runtime.regex.NamedPattern;
import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings({"FieldCanBeLocal"}) class AsmCodeEmitter implements Opcodes {
  private static final boolean printBytecode = System.getProperty("print_bytecode") != null;
  private static final AtomicInteger functionNameSequence = new AtomicInteger();

  private static final String IS_LIST_VAR_PREFIX = "__$isList_";
  private static final String RUNTIME_LIST_SIZE_VAR_PREFIX = "__$runtimeListSize_";
  private static final String RUNTIME_STR_LEN_PREFIX = "__$str_len_";
  private static final String IS_STRING_PREFIX = "__$isStr_";
  private static final String IS_READER_PREFIX = "__$isRdr_";
  private static final String WHERE_SCOPE_FN_PREFIX = "$wh$";

  private final Stack<Context> functionStack = new Stack<Context>();

  private final Scope scope;

  public static class SourceLocation implements Comparable<SourceLocation> {
    public final int line;
    public final int column;

    public SourceLocation(int line, int column) {
      this.line = line;
      this.column = column;
    }

    @Override
    public int compareTo(SourceLocation that) {
      return (this.line * 1000000 + this.column) - (that.line * 1000000 + that.column);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SourceLocation that = (SourceLocation) o;

      return column == that.column && line == that.line;

    }

    @Override
    public int hashCode() {
      return 31 * line + column;
    }
  }

  private final Map<Class<?>, Emitter> EMITTERS = new HashMap<Class<?>, Emitter>();

  AsmCodeEmitter(Scope scope) {
    this.scope = scope;

    EMITTERS.put(Call.class, callEmitter);
    EMITTERS.put(Dereference.class, dereferenceEmitter);
    EMITTERS.put(Computation.class, computationEmitter);
    EMITTERS.put(IntLiteral.class, intEmitter);
    EMITTERS.put(FloatLiteral.class, floatEmitter);
    EMITTERS.put(LongLiteral.class, longEmitter);
    EMITTERS.put(DoubleLiteral.class, doubleEmitter);
    EMITTERS.put(BigIntegerLiteral.class, bigIntegerEmitter);
    EMITTERS.put(BigDecimalLiteral.class, bigDecimalEmitter);
    EMITTERS.put(BooleanLiteral.class, booleanEmitter);
    EMITTERS.put(TypeLiteral.class, typeLiteralEmitter);
    EMITTERS.put(Variable.class, variableEmitter);
    EMITTERS.put(JavaLiteral.class, javaLiteralEmitter);
    EMITTERS.put(BinaryOp.class, binaryOpEmitter);
    EMITTERS.put(StringLiteral.class, stringLiteralEmitter);
    EMITTERS.put(RegexLiteral.class, regexLiteralEmitter);
    EMITTERS.put(Assignment.class, assignmentEmitter);
    EMITTERS.put(InlineMapDef.class, inlineMapEmitter);
    EMITTERS.put(InlineListDef.class, inlineListEmitter);
    EMITTERS.put(IndexIntoList.class, indexIntoListEmitter);
    EMITTERS.put(CallChain.class, callChainEmitter);
    EMITTERS.put(FunctionDecl.class, functionDeclEmitter);
    EMITTERS.put(ArgDeclList.class, argDeclEmitter);
    EMITTERS.put(PrivateField.class, privateFieldEmitter);
    EMITTERS.put(PatternRule.class, patternRuleEmitter);
    EMITTERS.put(TernaryIfExpression.class, ternaryExpressionEmitter);
    EMITTERS.put(TernaryUnlessExpression.class, ternaryExpressionEmitter);
    EMITTERS.put(Comprehension.class, comprehensionEmitter);
    EMITTERS.put(ConstructorCall.class, constructorCallEmitter);
    EMITTERS.put(ListRange.class, inlineListRangeEmitter);
  }

  private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
  private final Stack<MethodVisitor> methodStack = new Stack<MethodVisitor>();

  public Class<?> write(Unit unit) {
    Thread.currentThread().setContextClassLoader(LoopClassLoader.CLASS_LOADER);

    // We always emit functions as static into a containing Java class.
    String javaClass = unit.name();

    String fileName = unit.getFileName();
    if (fileName != null) {
      if (!fileName.endsWith(".loop"))
        fileName += ".loop";
      classWriter.visitSource(fileName, null);
    }
    classWriter.visit(V1_6, ACC_PUBLIC, javaClass, null, "java/lang/Object", new String[0]);

    for (FunctionDecl functionDecl : unit.functions()) {
      emit(functionDecl);
    }

    // Emit any static initializer here.
    if (unit.initializer() != null)
      emitInitializerBlock(unit.initializer());

    classWriter.visitEnd();

    if (printBytecode) {
      try {
        new ClassReader(new ByteArrayInputStream(classWriter.toByteArray())).accept(
            new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.SKIP_DEBUG);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    LoopClassLoader.CLASS_LOADER.put(javaClass, classWriter.toByteArray());
    try {
      return LoopClassLoader.CLASS_LOADER.findClass(javaClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void emitInitializerBlock(List<Node> exprs) {
    MethodVisitor initializer = classWriter.visitMethod(ACC_STATIC,
        "<clinit>",
        "()V",
        null,
        null);

    methodStack.push(initializer);
    Context context = new Context(FunctionDecl.STATIC_INITIALIZER);
    functionStack.push(context);
    scope.pushScope(context);
    for (Node expr : exprs) {
      emit(expr);
      initializer.visitInsn(POP);
    }

    initializer.visitInsn(RETURN);
    initializer.visitMaxs(1, 0);
    initializer.visitEnd();
    scope.popScope();
    functionStack.pop();
    methodStack.pop();
  }

  private void trackLineAndColumn(Node node) {
    Label line = new Label();
    methodStack.peek().visitLabel(line);
    methodStack.peek().visitLineNumber(node.sourceLine, line);
  }

  private void emitChildren(Node node) {
    for (Node child : node.children()) {
      emit(child);
    }
  }

  private void emitOnlyChild(Node node) {
    emit(node.children().get(0));
  }

  public void emit(Node node) {
    if (!EMITTERS.containsKey(node.getClass()))
      throw new RuntimeException("Missing emitter for " + node.getClass().getSimpleName());
    EMITTERS.get(node.getClass()).emitCode(node);
  }

  // -------------------------------------------------------------------
  // EMITTERS ----------------------------------------------------------
  // -------------------------------------------------------------------

  private final Emitter ternaryExpressionEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      boolean unless = node instanceof TernaryUnlessExpression;
      MethodVisitor methodVisitor = methodStack.peek();

      Label elseBranch = new Label();
      Label end = new Label();

      // If condition
      emit(node.children().get(0));
      methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");

      // Flip the clauses in an "unless" expression.
      if (unless)
        methodVisitor.visitJumpInsn(IFNE, elseBranch);
      else
        methodVisitor.visitJumpInsn(IFEQ, elseBranch);

      emit(node.children().get(1));
      methodVisitor.visitJumpInsn(GOTO, end);

      methodVisitor.visitLabel(elseBranch);
      emit(node.children().get(2));
      methodVisitor.visitLabel(end);
    }
  };

  private final Emitter computationEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      trackLineAndColumn(node);
      emitChildren(node);
    }
  };

  private final Emitter dereferenceEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Dereference dereference = (Dereference) node;
      trackLineAndColumn(dereference);

      MethodVisitor methodVisitor = methodStack.peek();
      Context context = functionStack.peek();

      methodVisitor.visitLdcInsn(dereference.name());

      // Special form to call on a java type rather than lookup by class name.
      if (dereference.isJavaStatic()) {
        methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "getStatic",
            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;");
      } else {
        // If JDK7, use invokedynamic instead for better performance.
        methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "dereference",
            "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
      }
    }
  };

  private final Emitter callEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Call call = (Call) node;
      trackLineAndColumn(call);

      MethodVisitor methodVisitor = methodStack.peek();
      Context context = functionStack.peek();

      FunctionDecl resolvedFunction;
      if (call.namespace() != null)
        resolvedFunction = scope.resolveNamespacedFunction(call.name(), call.namespace());
      else
        resolvedFunction = call.callJava() ? null : scope.resolveFunctionOnStack(call.name());

      // All Loop functions are Java static.
      boolean isStatic = resolvedFunction != null, isClosure = false;

      // Is this a tail-recursive function?
      boolean isTailRecursive = call.isTailCall()
          && call.namespace() == null
          && !call.isJavaStatic()
          && context.thisFunction.equals(resolvedFunction);

      // The parse-tree knows if we are calling a java method statically.
      if (!isStatic)
        isStatic = call.isJavaStatic();

      // This is a special invocation so we emit it without the dot.
      String name;
      if (Closure.CALL_FORM.equals(call.name())) {
        name = "";

        isStatic = true;
        isClosure = true;
      } else if (isStatic && resolvedFunction != null) {
        name = normalizeMethodName(resolvedFunction.scopedName());
      } else
        name = normalizeMethodName(call.name());

      // Compute if we should "call as postfix method" (can be overridden with <- operator)
      List<Node> arguments = call.args().children();
      int callAsPostfixVar = -1;

      boolean callAsPostfix = false;
      int argSize = arguments.size();
      if (resolvedFunction != null && resolvedFunction.arguments() != null) {
        // The actual call is 1 less argument than the function takes, so this is a
        // "call-as-method" syntax.
        if (resolvedFunction.arguments().children().size() - argSize == 1) {
          callAsPostfix = true;
          callAsPostfixVar = context.localVarIndex(context.newLocalVariable());
          argSize++;

          // Save the top of the stack for use as the first argument.
          methodVisitor.visitVarInsn(ASTORE, callAsPostfixVar);
        }
      }

      // TAIL CALL ELIMINATION:
      // Store the call-args into the args of this function and short-circuit the call.
      if (isTailRecursive) {
        List<Node> children = call.args().children();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
          Node arg = children.get(i);
          emit(arg);                                        // value

          // Store into the local vars representing the arguments to the recursive function.
          methodVisitor.visitVarInsn(ASTORE, i);
        }

        // If there's anything left, pop it off. If some expression results in void on stack
        // rather than null or 0, then we might be screwed.
        // Pattern matching functions are well behaved (i.e. not multiline, so leave them alone)
        if (!context.thisFunction.patternMatching && context.thisFunction.children().size() > 1) {
          for (int i = 1; i < context.thisFunction.children().size(); i++) {
            methodVisitor.visitInsn(POP);
          }
        }

        // Loop.
        methodVisitor.visitJumpInsn(GOTO, context.startOfFunction);

        return;
      }

      // push name of containing type if this is a static call.
      boolean isExternalFunction = resolvedFunction != null
          && resolvedFunction.moduleName != null
          && !scope.getModuleName().equals(resolvedFunction.moduleName);

      if (isStatic && !call.isJavaStatic()) {
        if (isClosure)
          methodVisitor.visitTypeInsn(CHECKCAST, "loop/runtime/Closure");

        if (!isExternalFunction)
          methodVisitor.visitLdcInsn(scope.getModuleName());
      }

      // push method name onto stack
      if (!isClosure) {
        // Emit the module name of the containing class for the resolved function, BUT only
        // if it is not in the same module as us.
        if (isExternalFunction) {
          methodVisitor.visitLdcInsn(resolvedFunction.moduleName);
        }

        methodVisitor.visitLdcInsn(name);
      }

      if (argSize > 0) {
        int arrayVar = context.localVarIndex(context.newLocalVariable());

        // push args as array.
        methodVisitor.visitIntInsn(BIPUSH, argSize);       // size of array
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        methodVisitor.visitVarInsn(ASTORE, arrayVar);
        int i = 0;

        if (callAsPostfix) {
          methodVisitor.visitVarInsn(ALOAD, arrayVar);          // array
          methodVisitor.visitIntInsn(BIPUSH, i);                // index
          methodVisitor.visitVarInsn(ALOAD, callAsPostfixVar);  // value
          methodVisitor.visitInsn(AASTORE);

          i++;
        }

        for (Node arg : arguments) {
          methodVisitor.visitVarInsn(ALOAD, arrayVar);      // array
          methodVisitor.visitIntInsn(BIPUSH, i);            // index
          emit(arg);                                        // value

          methodVisitor.visitInsn(AASTORE);
          i++;
        }

        // Load the array back in.
        methodVisitor.visitVarInsn(ALOAD, arrayVar);

        if (isStatic) {
          // If JDK7, use invokedynamic instead for better performance.
          if (isClosure)
            methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callClosure",
                "(Lloop/runtime/Closure;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
          else {
            // Special form to call on a java type rather than lookup by class name.
            if (call.callJava()) {
              methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
                "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            } else {
              methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
                  "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            }
          }

        } else {
          // If JDK7, use invokedynamic instead for better performance.
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "call",
              "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        }
      } else {
        if (isStatic) {
          // If JDK7, use invokedynamic instead for better performance.
          if (isClosure)
            methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callClosure",
                "(Lloop/runtime/Closure;Ljava/lang/String;)Ljava/lang/Object;");
          else {
            // Special form to call on a java type rather than lookup by class name.
            if (call.callJava()) {
              methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
                  "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Object;");
            } else {
              methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
                  "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
            }
          }

        } else {
          // If JDK7, use invokedynamic instead for better performance.
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "call",
              "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        }
      }
    }
  };

  private final Emitter constructorCallEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      ConstructorCall call = (ConstructorCall) node;
      MethodVisitor methodVisitor = methodStack.peek();
      Context context = functionStack.peek();

      // Try to resolve as an aliased dep first.
      ClassDecl classDecl = null;
      if (call.modulePart != null && call.modulePart.split("[.]").length == 1)
        classDecl = scope.resolveAliasedType(
            call.modulePart.substring(0, call.modulePart.length() - 1),
            call.name);

      // Resolve a loop type internally. Note that this makes dynamic linking
      // of Loop types impossible, but we CAN link Java binaries dynamically.
      if (classDecl == null)
        classDecl = scope.resolve(call.name, true);
      if (classDecl != null) {

        // Instatiate the loop object first. With the correct type
        int objectVar = context.localVarIndex(context.newLocalVariable());

        methodVisitor.visitTypeInsn(NEW, "loop/lang/LoopObject");
        methodVisitor.visitInsn(DUP);

        methodVisitor.visitTypeInsn(NEW, "loop/lang/LoopClass");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitLdcInsn(classDecl.name);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "loop/lang/LoopClass", "<init>",
            "(Ljava/lang/String;)V");

        methodVisitor.visitMethodInsn(INVOKESPECIAL, "loop/lang/LoopObject", "<init>",
            "(Lloop/lang/LoopClass;)V");
        methodVisitor.visitVarInsn(ASTORE, objectVar);


        Map<String, Node> fields = new HashMap<String, Node>();
        for (Node nodeAssign : classDecl.children()) {
          if (nodeAssign instanceof Assignment) {
            Assignment assignment = (Assignment) nodeAssign;
            fields.put(((Variable) assignment.lhs()).name, assignment.rhs());
          }
        }

        List<Node> children = call.args().children();
        if (!children.isEmpty() || !fields.isEmpty()) {

          // First emit named-args as overrides of defaults.
          for (Node child : children) {
            CallArguments.NamedArg arg = (CallArguments.NamedArg) child;

            methodVisitor.visitVarInsn(ALOAD, objectVar);
            methodVisitor.visitLdcInsn(arg.name);
            emit(arg.arg);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            // Puts return crap which we need to discard.
            methodVisitor.visitInsn(POP);
          }

          // Now emit any remaining defaults.
          for (Map.Entry<String, Node> field : fields.entrySet()) {
            methodVisitor.visitVarInsn(ALOAD, objectVar);
            methodVisitor.visitLdcInsn(field.getKey());
            emit(field.getValue());
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            // Puts return crap which we need to discard.
            methodVisitor.visitInsn(POP);
          }
        }

        // Leave the object on the stack.
        methodVisitor.visitVarInsn(ALOAD, objectVar);

        // Should we freeze this object?
        if (classDecl.immutable) {
          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "loop/lang/LoopObject", "immutize",
              "()Lloop/lang/ImmutableLoopObject;");

          // Overwrite the old var in case there is a reference later on.
          methodVisitor.visitVarInsn(ASTORE, objectVar);
          methodVisitor.visitVarInsn(ALOAD, objectVar);
        }

      } else {
        // Emit Java constructor call.
        String javaType;
        if (call.modulePart != null)
          javaType = call.modulePart + call.name;
        else
          javaType = scope.resolveJavaType(call.name);

        boolean isNullary = call.args().children().isEmpty();
        if (!isNullary) {
          int arrayVar = context.localVarIndex(context.newLocalVariable());
          methodVisitor.visitIntInsn(BIPUSH, call.args().children().size());       // size of array
          methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          methodVisitor.visitVarInsn(ASTORE, arrayVar);

          int i = 0;
          for (Node arg : call.args().children()) {
            methodVisitor.visitVarInsn(ALOAD, arrayVar);      // array
            methodVisitor.visitIntInsn(BIPUSH, i);            // index
            emit(arg);                                        // value

            methodVisitor.visitInsn(AASTORE);
            i++;
          }

          // push type and constructor arg array.
          methodVisitor.visitLdcInsn(javaType);
          methodVisitor.visitVarInsn(ALOAD, arrayVar);

          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "instantiate",
              "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
          // Otherwise invoke the Java constructor directly. We don't need to resolve it.
          // This is an optimization.
          javaType = javaType.replace('.', '/');
          methodVisitor.visitTypeInsn(NEW, javaType);
          methodVisitor.visitInsn(DUP);
          methodVisitor.visitMethodInsn(INVOKESPECIAL, javaType, "<init>", "()V");
        }
      }
    }
  };

  private final Emitter binaryOpEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BinaryOp binaryOp = (BinaryOp) node;
      emitOnlyChild(binaryOp);

      MethodVisitor methodVisitor = methodStack.peek();
      switch (binaryOp.operator.kind) {
        case PLUS:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "plus",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          break;
        case MINUS:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "minus",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          break;
        case STAR:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "multiply",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          break;
        case DIVIDE:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "divide",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          break;
        case MODULUS:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "remainder",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          break;
        case LESSER:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "lesserThan",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        case LEQ:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "lesserThanOrEqual",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        case GREATER:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "greaterThan",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        case GEQ:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations",
              "greaterThanOrEqual",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        case EQUALS:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "equal",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        case AND:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "and",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
//          methodVisitor.visitInsn(IAND);
          break;
        case OR:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "or",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
//          methodVisitor.visitInsn(IOR);
          break;
        case NOT:
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "notEqual",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          break;
        default:
          throw new UnsupportedOperationException(
              "Unsupported binary operator " + binaryOp.toSymbol());
      }
    }
  };

  private final Emitter assignmentEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Assignment assignment = (Assignment) node;
      trackLineAndColumn(assignment);
      Context context = functionStack.peek();
      MethodVisitor methodVisitor = methodStack.peek();

      if (assignment.lhs() instanceof Variable) {
        int lhsVar = context.localVarIndex(context.newLocalVariable((Variable) assignment.lhs()));
        emit(assignment.rhs());
        methodVisitor.visitVarInsn(ASTORE, lhsVar);
      } else if (assignment.lhs() instanceof CallChain) {
        // this is a setter/put style assignment.
        CallChain lhs = (CallChain) assignment.lhs();
        List<Node> children = lhs.children();
        Node last = children.remove(children.size() - 1);

        if (last instanceof IndexIntoList) {
          // The object to assign into.
          emit(lhs);

          // The slot where this assignment will go.
          emit(((IndexIntoList) last).from());

          // The value to assign.
          emit(assignment.rhs());
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "store",
              "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        } else if (last instanceof MemberAccess) {
          MemberAccess call = (MemberAccess) last;
          if (call instanceof Call)
            throw new RuntimeException("Cannot assign value to a function call");

          // The object to assign into.
          emit(lhs);

          // The slot where this assignment will go.
          methodVisitor.visitLdcInsn(call.name());

          // The value to assign.
          emit(assignment.rhs());
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "store",
              "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        } else
          throw new RuntimeException("Can only assign a value to variable or object property");
      }
    }
  };

  private final Emitter variableEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Variable var = (Variable) node;

      Context context = functionStack.peek();
      Integer index = context.argumentIndex.get(var.name);
      if (index == null)
        index = context.localVarIndex(var.name);

      if (index == null) {
        // It could be a function-reference. In which case emit it as a closure.
        FunctionDecl functionDecl = scope.resolveFunctionOnStack(var.name);
        if (functionDecl != null) {
          MethodVisitor methodVisitor = methodStack.peek();
          methodVisitor.visitTypeInsn(NEW, "loop/runtime/Closure");
          methodVisitor.visitInsn(DUP);
          methodVisitor.visitLdcInsn(functionDecl.moduleName);
          methodVisitor.visitLdcInsn(functionDecl.scopedName());
          methodVisitor.visitMethodInsn(INVOKESPECIAL, "loop/runtime/Closure", "<init>",
              "(Ljava/lang/String;Ljava/lang/String;)V");

        }
      } else
        methodStack.peek().visitVarInsn(ALOAD, index);
    }
  };

  private static String normalizeMethodName(String name) {
    return name.replaceFirst("@", "__");
  }

  private final Emitter intEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      IntLiteral intLiteral = (IntLiteral) node;

      // Emit int wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitLdcInsn(intLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
          "(I)Ljava/lang/Integer;");

    }
  };

  private final Emitter floatEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      FloatLiteral floatLiteral = (FloatLiteral) node;

      // Emit float wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitLdcInsn(floatLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf",
          "(F)Ljava/lang/Float;");
    }
  };

  private final Emitter longEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      LongLiteral longLiteral = (LongLiteral) node;

      // Emit long wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitLdcInsn(longLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf",
          "(J)Ljava/lang/Long;");
    }
  };

  private final Emitter doubleEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      DoubleLiteral doubleLiteral = (DoubleLiteral) node;

      // Emit double wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitLdcInsn(doubleLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf",
          "(D)Ljava/lang/Double;");
    }
  };

  private final Emitter bigIntegerEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BigIntegerLiteral bigIntegerLiteral = (BigIntegerLiteral) node;

      // Emit bigint wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitTypeInsn(NEW, "java/math/BigInteger");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bigIntegerLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>",
          "(Ljava/lang/String;)V");
    }
  };

  private final Emitter bigDecimalEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BigDecimalLiteral bigDecimalLiteral = (BigDecimalLiteral) node;

      // Emit bigdecimal wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitTypeInsn(NEW, "java/math/BigDecimal");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitLdcInsn(bigDecimalLiteral.value);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>",
          "(Ljava/lang/String;)V");
    }
  };

  private final Emitter booleanEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BooleanLiteral booleanLiteral = (BooleanLiteral) node;

      // Emit boolean wrappers.
      MethodVisitor methodVisitor = methodStack.peek();
      methodVisitor.visitIntInsn(BIPUSH, booleanLiteral.value ? 1 : 0);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf",
          "(Z)Ljava/lang/Boolean;");

    }
  };

  private final Emitter typeLiteralEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      TypeLiteral type = (TypeLiteral) node;
      trackLineAndColumn(type);

      MethodVisitor methodVisitor = methodStack.peek();
      if (TypeLiteral.NOTHING.equals(type.name)) // Special-case the bottom type.
        methodVisitor.visitInsn(ACONST_NULL);
      else {
        ClassDecl classDecl = scope.resolve(type.name, true);

        if (classDecl != null)
          throw new UnsupportedOperationException(); // TODO reflection.

        String fqn = scope.resolveJavaType(type.name);
        if (fqn != null) {
          methodVisitor.visitLdcInsn(fqn);
          methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName",
              "(Ljava/lang/String;)Ljava/lang/Class;");
        }
      }
    }
  };

  private final Emitter javaLiteralEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      JavaLiteral java = (JavaLiteral) node;
      trackLineAndColumn(java);

      MethodVisitor methodVisitor = methodStack.peek();

      if (java.staticFieldAccess == null)
        methodVisitor.visitLdcInsn(java.value);
      else {
        methodVisitor.visitLdcInsn(java.value);
        methodVisitor.visitLdcInsn(java.staticFieldAccess);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "getStatic",
            "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
      }
    }
  };

  private final Emitter stringLiteralEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      StringLiteral string = (StringLiteral) node;
      trackLineAndColumn(string);

      if (string.parts != null) {
        MethodVisitor methodVisitor = methodStack.peek();

        // Emit stringbuilder for string interpolation.
        methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");

        for (Node part : string.parts) {
          if (part instanceof StringLiteral)
            methodVisitor.visitLdcInsn(((StringLiteral) part).value);
          else
            emit(part);

          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
              "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        }

        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
            "()Ljava/lang/String;");

      } else {
        methodStack.peek().visitLdcInsn(string.value.substring(1, string.value.length() - 1));
      }
    }
  };

  private final Emitter regexLiteralEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      throw new UnsupportedOperationException();
    }
  };

  private final Emitter functionDeclEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      FunctionDecl functionDecl = (FunctionDecl) node;
      String name = functionDecl.scopedName();
      boolean isClosure = functionDecl.isAnonymous();
      if (isClosure) {
        // Function is anonymous, generate a globally unique name for it.
        name = "$fn_" + functionNameSequence.incrementAndGet();
      }
      Context innerContext = new Context(functionDecl);

      // Before we emit the body of this method into the class scope, let's
      // see if this is closure, and if it is, emit it as a function reference.
      List<Variable> freeVariables;
      if (isClosure) {
        MethodVisitor currentVisitor = methodStack.peek();

        // Discover any free variables and save them to this closure.
        freeVariables = new ArrayList<Variable>();
        detectFreeVariables(functionDecl, functionDecl.arguments(), freeVariables);
        functionDecl.freeVariables = freeVariables;

        // Add them to the argument list of the function.
        for (Variable freeVariable : freeVariables) {
          functionDecl.arguments().add(new ArgDeclList.Argument(freeVariable.name, null));
        }

        currentVisitor.visitTypeInsn(NEW, "loop/runtime/Closure");
        currentVisitor.visitInsn(DUP);
        currentVisitor.visitLdcInsn(functionDecl.moduleName);
        currentVisitor.visitLdcInsn(name);

        if (!freeVariables.isEmpty()) {
          Context outerContext = functionStack.peek();
          int arrayIndex = outerContext.localVarIndex(outerContext.newLocalVariable());

          // push free variables as array.
          currentVisitor.visitIntInsn(BIPUSH, freeVariables.size());       // size of array
          currentVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          currentVisitor.visitVarInsn(ASTORE, arrayIndex);
          int i = 0;
          for (Variable freeVariable : freeVariables) {
            currentVisitor.visitVarInsn(ALOAD, arrayIndex);    // array
            currentVisitor.visitIntInsn(BIPUSH, i);            // index
            emit(freeVariable);                                // value

            currentVisitor.visitInsn(AASTORE);
            i++;
          }

          // Load the array back in.
          currentVisitor.visitVarInsn(ALOAD, arrayIndex);
          currentVisitor.visitMethodInsn(INVOKESPECIAL, "loop/runtime/Closure", "<init>",
              "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");
        } else
          currentVisitor.visitMethodInsn(INVOKESPECIAL, "loop/runtime/Closure", "<init>",
              "(Ljava/lang/String;Ljava/lang/String;)V");
      }


      //******* BEGIN FUNCTION SIGNATURE ********


      // Start writing this function in its own scope.
      StringBuilder args = new StringBuilder("(");
      List<Node> children = functionDecl.arguments().children();
      for (int functionArgIndex = 0, childrenSize = children.size(); functionArgIndex < childrenSize; functionArgIndex++) {
        Node arg = children.get(functionArgIndex);
        String argName = ((ArgDeclList.Argument) arg).name();
        innerContext.arguments.add(argName);
        innerContext.argumentIndex.put(argName, functionArgIndex);

        args.append("Ljava/lang/Object;");
      }
      args.append(")");
      functionStack.push(innerContext);
      scope.pushScope(innerContext);

      final MethodVisitor methodVisitor = classWriter.visitMethod(
          (functionDecl.isPrivate ? ACC_PRIVATE : ACC_PUBLIC) + ACC_STATIC,
          normalizeMethodName(name),
          args.append("Ljava/lang/Object;").toString(),
          null,
          null);
      methodStack.push(methodVisitor);
      trackLineAndColumn(functionDecl);

      methodVisitor.visitLabel(innerContext.startOfFunction);

      //******* BEGIN CELL TRANSACTION ********
      if (functionDecl.cell != null) {
        int thisIndex = innerContext.newLocalVariable("this");

        // Load the cell in a transactional wrapper into the "this" variable.
        methodVisitor.visitLdcInsn(functionDecl.cell);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Cells", "beginTransaction",
            "(Ljava/lang/String;)Ljava/lang/Object;");
        methodVisitor.visitVarInsn(ASTORE, thisIndex);
      }

      //******* BEGIN WHERE BLOCK LOCALS ********

      // Emit static definitions in all parent where blocks.
      for (int i1 = 0, functionStackSize = functionStack.size(); i1 < functionStackSize - 1; i1++) {
        FunctionDecl parent = functionStack.get(i1).thisFunction;

        for (Node helper : parent.whereBlock()) {
          if (helper instanceof Assignment)
            emit(helper);
        }
      }

      // Emit locally-scoped helper functions and variables.
      for (Node helper : functionDecl.whereBlock()) {
        // Rewrite helper functions to be namespaced inside the parent function.
        if (helper instanceof FunctionDecl)
          scopeNestedFunction(functionDecl, innerContext, (FunctionDecl) helper);
        emit(helper);
      }

      //******* BEGIN PATTERN HELPER LOCALS ********

      // Set up some helper local vars to make it easier to pattern match certain types (lists).
      if (functionDecl.patternMatching) {
        boolean checkIfLists = false, checkIfString = false;
        for (Node child : functionDecl.children()) {
          PatternRule patternRule = (PatternRule) child;

          for (Node pattern : patternRule.patterns) {
            if (pattern instanceof ListDestructuringPattern ||
                pattern instanceof ListStructurePattern) {
              checkIfLists = true;
            } else if (pattern instanceof StringPattern) {
              checkIfString = true;
            }
          }
        }

        if (checkIfLists) {
          List<Node> children1 = functionDecl.arguments().children();
          for (int i = 0, children1Size = children1.size(); i < children1Size; i++) {
            int isList = innerContext.newLocalVariable(IS_LIST_VAR_PREFIX + i);
            int runtimeListSize = innerContext.newLocalVariable(RUNTIME_LIST_SIZE_VAR_PREFIX + i);

            methodVisitor.visitVarInsn(ALOAD, i);
            methodVisitor.visitTypeInsn(INSTANCEOF, "java/util/List");
            methodVisitor.visitIntInsn(ISTORE, isList);

            // Initialize register for local var.
            methodVisitor.visitIntInsn(BIPUSH, -1);
            methodVisitor.visitIntInsn(ISTORE, runtimeListSize);

            Label notAList = new Label();
            methodVisitor.visitIntInsn(ILOAD, isList);
            methodVisitor.visitJumpInsn(IFEQ, notAList);

            methodVisitor.visitVarInsn(ALOAD, i);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
            methodVisitor.visitIntInsn(ISTORE, runtimeListSize);
            methodVisitor.visitLabel(notAList);
          }
        }
        if (checkIfString) {
          for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            int isString = innerContext.newLocalVariable(IS_STRING_PREFIX + i);
            int isReader = innerContext.newLocalVariable(IS_READER_PREFIX + i);
            int runtimeStringLen = innerContext.newLocalVariable(RUNTIME_STR_LEN_PREFIX + i);

            // Initialize all local vars we're going to use.
            methodVisitor.visitIntInsn(BIPUSH, 0);
            methodVisitor.visitVarInsn(ISTORE, isString);
            methodVisitor.visitIntInsn(BIPUSH, 0);
            methodVisitor.visitVarInsn(ISTORE, isReader);
            methodVisitor.visitIntInsn(BIPUSH, -1);
            methodVisitor.visitVarInsn(ISTORE, runtimeStringLen);

            methodVisitor.visitVarInsn(ALOAD, i);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitTypeInsn(INSTANCEOF, "java/lang/String");
            methodVisitor.visitIntInsn(ISTORE, isString);
            methodVisitor.visitTypeInsn(INSTANCEOF, "java/io/Reader");
            methodVisitor.visitIntInsn(ISTORE, isReader);
            methodVisitor.visitIntInsn(BIPUSH, 1);

            Label skipStringChecks = new Label();
            methodVisitor.visitJumpInsn(IFNE, skipStringChecks);
            methodVisitor.visitVarInsn(ALOAD, i);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
            methodVisitor.visitIntInsn(ISTORE, runtimeStringLen);

            methodVisitor.visitLabel(skipStringChecks);
          }
        }
      }
      //******* BEGIN FUNCTION BODY ********

      //******* BEGIN TRY ********
      Map<String, Label> catchBlocks = new LinkedHashMap<String, Label>();
      FunctionDecl exceptionHandler = null;
      if (functionDecl.exceptionHandler != null) {
        exceptionHandler = scope.resolveFunctionOnStack(functionDecl.exceptionHandler);

        Label tryStart = new Label();

        // Determine exception types.
        Label firstCatchStart = null;
        for (String type : exceptionHandler.handledExceptions()) {
          Label catchStart = new Label();
          catchBlocks.put(type, catchStart);

          if (null == firstCatchStart)
            firstCatchStart = catchStart;

          // Resolve java exception type, against imported types.
          String resolvedType = scope.resolveJavaType(type);
          if (resolvedType != null)
            type = resolvedType;

          methodVisitor.visitTryCatchBlock(tryStart, firstCatchStart, catchStart, type.replace('.', '/'));
        }
        methodVisitor.visitLabel(tryStart);
      }

      //******* BEGIN INSTRUCTIONS ********


      emitChildren(node);

      if (functionDecl.patternMatching) {
        methodVisitor.visitLdcInsn("Non-exhaustive pattern rules in " + functionDecl.name());
        methodVisitor.visitInsn(DUP);   // This is necessary just to maintain stack height consistency. =/
        methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/Loop", "error", "(Ljava/lang/String;)V");
      }

      methodVisitor.visitLabel(innerContext.endOfFunction);
      methodVisitor.visitInsn(ARETURN);

      //******* END FUNCTION BODY ********

      if (functionDecl.exceptionHandler != null) {
        for (Map.Entry<String, Label> typeLabel : catchBlocks.entrySet()) {
          methodVisitor.visitLabel(typeLabel.getValue());

          // Emit call to handler.
          // TODO probably need to resolve this to the correct module.
          methodVisitor.visitMethodInsn(INVOKESTATIC, scope.getModuleName(),
              exceptionHandler.scopedName(), "(Ljava/lang/Object;)Ljava/lang/Object;");
          methodVisitor.visitInsn(ARETURN);
        }
      }

      methodVisitor.visitMaxs(0, 0);
      methodVisitor.visitEnd();

      methodStack.pop();
      functionStack.pop();
      scope.popScope();
    }
  };

  private void detectFreeVariables(Node top, ArgDeclList args, List<Variable> vars) {
    // Pre-order traversal.
    for (Node node : top.children()) {
      detectFreeVariables(node, args, vars);
    }

    if (top instanceof Variable) {
      Variable local = (Variable) top;

      boolean free = true;
      if (args != null)
        for (Node arg : args.children()) {
          ArgDeclList.Argument argument = (ArgDeclList.Argument) arg;

          if (argument.name().equals(local.name))
            free = false;
        }

      if (free)
        vars.add(local);

    } else if (top instanceof Call) {
      Call call = (Call) top;
      detectFreeVariables(call.args(), args, vars);
    }

  }

  private void scopeNestedFunction(FunctionDecl parent, Context context, FunctionDecl function) {
    String unscopedName = function.name();

    String newName;
    if (parent.name().startsWith(WHERE_SCOPE_FN_PREFIX))
      newName = parent.name() + '$' + unscopedName;
    else
      newName = WHERE_SCOPE_FN_PREFIX + parent.name() + '$' + unscopedName;

    // Apply the scoped name globally.
    function.scopedName(newName);
    context.newLocalFunction(unscopedName, function);
  }

  private final Emitter privateFieldEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      PrivateField privateField = (PrivateField) node;
      trackLineAndColumn(privateField);
      String name = privateField.name().substring(1); // Strip @

      methodStack.peek().visitLdcInsn(name);
    }
  };

  private final Emitter argDeclEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
    }
  };

  private final Emitter inlineListEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      InlineListDef inlineListDef = (InlineListDef) node;

      MethodVisitor methodVisitor = methodStack.peek();
      Context context = functionStack.peek();

      String listType = inlineListDef.isSet ? "java/util/HashSet" : "java/util/ArrayList";

      int listVar = context.localVarIndex(context.newLocalVariable());
      methodVisitor.visitTypeInsn(NEW, listType);
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, listType, "<init>", "()V");
      methodVisitor.visitVarInsn(ASTORE, listVar);

      for (Node child : inlineListDef.children()) {
        methodVisitor.visitVarInsn(ALOAD, listVar);
        emit(child);

        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add",
            "(Ljava/lang/Object;)Z");
        methodVisitor.visitInsn(POP); // discard result of add()
      }

      methodVisitor.visitVarInsn(ALOAD, listVar);
    }
  };

  private final Emitter inlineListRangeEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      ListRange range = (ListRange) node;

      MethodVisitor methodVisitor = methodStack.peek();

      emit(range.from);
      emit(range.to);
      methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "range",
          "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }
  };

  private final Emitter inlineMapEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      InlineMapDef inlineMapDef = (InlineMapDef) node;

      MethodVisitor methodVisitor = methodStack.peek();
      Context context = functionStack.peek();

      String mapType = inlineMapDef.isTree ? "java/util/TreeMap" : "java/util/HashMap";

      int mapVar = context.localVarIndex(context.newLocalVariable());
      methodVisitor.visitTypeInsn(NEW, mapType);
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, mapType, "<init>", "()V");
      methodVisitor.visitVarInsn(ASTORE, mapVar);

      for (Iterator<Node> iterator = inlineMapDef.children().iterator(); iterator.hasNext(); ) {
        Node key = iterator.next();
        methodVisitor.visitVarInsn(ALOAD, mapVar);
        emit(key);
        emit(iterator.next()); // value.

        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        methodVisitor.visitInsn(POP);   // disard put() result
      }

      methodVisitor.visitVarInsn(ALOAD, mapVar);
    }
  };


  private final Emitter callChainEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      CallChain callChain = (CallChain) node;
      List<Node> children = callChain.children();

      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        Node child = children.get(i);

        if (i == 0
            && callChain.nullSafe
            && child instanceof Variable
            && childrenSize > 1)
          trackLineAndColumn(child);
        emit(child);
      }
    }
  };

  private final Emitter indexIntoListEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      IndexIntoList indexIntoList = (IndexIntoList) node;
      MethodVisitor methodVisitor = methodStack.peek();

      Node from = indexIntoList.from();
      if (from != null)
        emit(from);

      boolean hasTo = indexIntoList.to() != null;
      if (hasTo) {
        emit(indexIntoList.to());

        if (indexIntoList.isSlice() && from == null)
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "sliceTo",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        else
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "obtain",
              "(Ljava/lang/Object;Ljava/lang/Integer;Ljava/lang/Integer;)Ljava/lang/Object;");
      } else {
        if (indexIntoList.isSlice())
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "sliceFrom",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        else
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "obtain",
              "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
      }
    }
  };

  private final Emitter comprehensionEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Comprehension comprehension = (Comprehension) node;

      Context context = functionStack.peek();
      MethodVisitor methodVisitor = methodStack.peek();

      // First create our output list.
      int outVarIndex = context.localVarIndex(context.newLocalVariable());

      methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");
      methodVisitor.visitInsn(DUP);
      methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      methodVisitor.visitVarInsn(ASTORE, outVarIndex);


      // Now loop through the target variable.
      int iVarIndex = context.localVarIndex(context.newLocalVariable());

      // iterator = collection.iterator()
      emit(comprehension.inList());
      methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");
      methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator",
          "()Ljava/util/Iterator;");
      methodVisitor.visitVarInsn(ASTORE, iVarIndex);

      Label start = new Label();
      Label end = new Label();
      // {
      // if !iterator.hasNext() jump to end
      methodVisitor.visitLabel(start);
      methodVisitor.visitVarInsn(ALOAD, iVarIndex);
      methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
      methodVisitor.visitJumpInsn(IFEQ, end);

      // var = iterator.next()
      methodVisitor.visitVarInsn(ALOAD, iVarIndex);
      methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next",
          "()Ljava/lang/Object;");
      int nextIndex = context.localVarIndex(context.newLocalVariable(comprehension.var()));
      methodVisitor.visitVarInsn(ASTORE, nextIndex);

      // if (filter_expression)
      if (comprehension.filter() != null) {
        emit(comprehension.filter());
        // Convert to primitive type.
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue",
            "()Z");
        methodVisitor.visitJumpInsn(IFEQ, start);
      }

      // Dump this value into the out list.
      methodVisitor.visitVarInsn(ALOAD, outVarIndex);
      methodVisitor.visitVarInsn(ALOAD, nextIndex);

      // Transform the value first using the projection expression.
      if (comprehension.projection() != null) {
        methodVisitor.visitInsn(POP);
        for (Node projection : comprehension.projection()) {
          emit(projection);
        }
      }

      methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add",
          "(Ljava/lang/Object;)Z");

      methodVisitor.visitInsn(POP);   // Discard result of add()
      methodVisitor.visitJumpInsn(GOTO, start);
      // }
      methodVisitor.visitLabel(end);

      // Finally.
      methodVisitor.visitVarInsn(ALOAD, outVarIndex);
    }
  };

  private void replaceVarInTree(Node top, Variable var, String with) {
    // Pre-order traversal.
    for (Node node : top.children()) {
      replaceVarInTree(node, var, with);
    }

    if (top instanceof Variable) {
      Variable local = (Variable) top;
      if (var.name.equals(local.name)) {
        local.name = with;
      }
    } else if (top instanceof Call) {
      Call call = (Call) top;
      replaceVarInTree(call.args(), var, with);
    }
  }

  private final Emitter patternRuleEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      PatternRule rule = (PatternRule) node;
      Context context = functionStack.peek();
      MethodVisitor methodVisitor = methodStack.peek();

      if (context.arguments.isEmpty())
        throw new RuntimeException("Incorrect number of arguments for pattern matching");

      if (context.arguments.size() != rule.patterns.size())
        throw new RuntimeException("Incorrect number of pattern rules. Expected pattern rules for "
            + context.arguments + " but found " + rule.patterns.size() + " rule(s): "
            + Parser.stringify(rule.patterns));

      Label matchedClause = new Label();
      Label endOfClause = new Label();

      for (int i = 0, argumentsSize = context.arguments.size(); i < argumentsSize; i++) {

        Node pattern = rule.patterns.get(i);
        if (pattern instanceof ListDestructuringPattern) {
          emitListDestructuringPatternRule(rule, methodVisitor, context, endOfClause,
              i);
        } else if (pattern instanceof ListStructurePattern) {
          emitListStructurePatternRule(rule, methodVisitor, context, matchedClause, endOfClause, i);
        } else if (pattern instanceof StringLiteral
            || pattern instanceof PrivateField
            || pattern instanceof IntLiteral
            || pattern instanceof BooleanLiteral) {

          methodVisitor.visitVarInsn(ALOAD, i);
          emit(pattern);

          if (!(pattern instanceof BooleanLiteral))
            methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Operations", "equal",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Boolean;");
          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");

          methodVisitor.visitJumpInsn(IFEQ, endOfClause);
        } else if (pattern instanceof TypeLiteral) {
          emitTypePatternRule(methodVisitor, matchedClause, endOfClause, i, (TypeLiteral) pattern);
        } else if (pattern instanceof RegexLiteral) {
          String regex = ((RegexLiteral) pattern).value;
          methodVisitor.visitLdcInsn(regex);

          // Discover named capturing groups if any.
          NamedPattern namedPattern = NamedPattern.compile(regex);
          List<String> groupNames = namedPattern.groupNames();
          for (String groupVarName : groupNames) {
            int varIndex = context.newLocalVariable(groupVarName);
            methodVisitor.visitInsn(ACONST_NULL);
            methodVisitor.visitVarInsn(ASTORE, varIndex);   // initialize part to null.
          }

          int matcherVar = context.localVarIndex(context.newLocalVariable());

          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/regex/NamedPattern", "compile",
              "(Ljava/lang/String;)Lloop/runtime/regex/NamedPattern;");

          methodVisitor.visitVarInsn(ALOAD, i);
          methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "loop/runtime/regex/NamedPattern", "matcher",
              "(Ljava/lang/CharSequence;)Lloop/runtime/regex/NamedMatcher;");

          methodVisitor.visitVarInsn(ASTORE, matcherVar);
          methodVisitor.visitVarInsn(ALOAD, matcherVar);
          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "loop/runtime/regex/NamedMatcher", "matches",
              "()Z");

          methodVisitor.visitJumpInsn(IFEQ, endOfClause);

          // Now extract the capturing group names.
          for (String groupNameVar : groupNames) {
            methodVisitor.visitVarInsn(ALOAD, matcherVar);
            methodVisitor.visitLdcInsn(groupNameVar);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "loop/runtime/regex/NamedMatcher", "group",
                "(Ljava/lang/String;)Ljava/lang/String;");
            methodVisitor.visitVarInsn(ASTORE, context.localVarIndex(groupNameVar));
          }

        } else if (pattern instanceof StringPattern) {
          emitStringPatternRule(rule, context, endOfClause, i);
        } else if (pattern instanceof MapPattern) {
          emitMapPatternRule(rule, context, matchedClause, endOfClause, i);
        } else if (pattern instanceof WildcardPattern) {
          // Always matches.
        }
      }

      methodVisitor.visitLabel(matchedClause);
      emitPatternClauses(rule);
      methodVisitor.visitJumpInsn(GOTO, context.endOfFunction);
      methodVisitor.visitLabel(endOfClause);
    }
  };

  private void emitTypePatternRule(MethodVisitor methodVisitor,
                                   Label matchedClause,
                                   Label endOfClause,
                                   int argIndex,
                                   TypeLiteral pattern) {
    String typeName;
    ClassDecl resolved = scope.resolve(pattern.name, true);
    if (resolved != null) {
      typeName = resolved.name;
    } else {
      typeName = scope.resolveJavaType(pattern.name);
    }

    methodVisitor.visitVarInsn(ALOAD, argIndex);
    methodVisitor.visitTypeInsn(INSTANCEOF, typeName.replace('.', '/'));
    methodVisitor.visitJumpInsn(IFEQ, endOfClause);
    methodVisitor.visitJumpInsn(GOTO, matchedClause);
  }

  private void emitPatternClauses(PatternRule rule) {
    if (rule.rhs != null) {
      emit(rule.rhs);
    } else
      emitGuards(rule);
  }

  private void emitGuards(PatternRule rule) {
    MethodVisitor methodVisitor = methodStack.peek();

    List<Node> children = rule.children();
    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
      Node node = children.get(i);
      if (!(node instanceof Guard))
        throw new RuntimeException("Apparent pattern rule missing guards: "
            + Parser.stringify(rule));
      Guard guard = (Guard) node;
      Label endOfClause = new Label();
      Label matchedClause = new Label();

      // The "Otherwise" expression is a plain else.
      boolean notElse = !(guard.expression instanceof OtherwiseGuard);
      if (notElse) {
        emit(guard.expression);

        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
        methodVisitor.visitJumpInsn(IFEQ, endOfClause);
      }

      methodVisitor.visitLabel(matchedClause);
      emit(guard.line);
      methodVisitor.visitJumpInsn(GOTO, functionStack.peek().endOfFunction);
      methodVisitor.visitLabel(endOfClause);

      if (i == childrenSize - 1 && notElse)
        methodVisitor.visitInsn(ACONST_NULL);
    }
  }

  private void emitMapPatternRule(PatternRule rule,
                                  Context context,
                                  Label matchedClause,
                                  Label endOfClause,
                                  int argIndex) {
    MapPattern pattern = (MapPattern) rule.patterns.get(argIndex);
    MethodVisitor methodVisitor = methodStack.peek();

    boolean hasType = false;
    List<Node> children = pattern.children();
    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
      Node child = children.get(i);
      if (child instanceof TypeLiteral) {
        hasType = true;

        TypeLiteral typeLiteral = (TypeLiteral) child;
        String typeName;
        ClassDecl resolved = scope.resolve(typeLiteral.name, true);
        if (resolved != null) {
          typeName = resolved.name;
        } else {
          typeName = scope.resolveJavaType(typeLiteral.name);
        }

        methodVisitor.visitVarInsn(ALOAD, argIndex);
        methodVisitor.visitTypeInsn(INSTANCEOF, typeName.replace('.', '/'));
        continue;
      }

      // Guard the first destructuring pair if there is a type pattern.
      if (hasType && i == 1) {
        methodVisitor.visitJumpInsn(IFEQ, endOfClause);
      }

      DestructuringPair pair = (DestructuringPair) child;

      int destructuredVar = context.localVarIndex(context.newLocalVariable((Variable) pair.lhs));
      emit(pair.rhs);

      methodVisitor.visitVarInsn(ASTORE, destructuredVar);
      methodVisitor.visitVarInsn(ALOAD, destructuredVar);
      methodVisitor.visitJumpInsn(IFNULL, endOfClause);
    }
    methodVisitor.visitJumpInsn(GOTO, matchedClause);
  }

  private void emitStringPatternRule(PatternRule rule,
                                     Context context,
                                     Label endOfClause,
                                     int argIndex) {
    MethodVisitor methodVisitor = methodStack.peek();

    methodVisitor.visitVarInsn(ILOAD, context.localVarIndex(IS_STRING_PREFIX + argIndex));
    methodVisitor.visitJumpInsn(IFEQ, endOfClause);   // Not a string, so skip

    List<Node> children = rule.patterns.get(argIndex).children();
    int i = 0, childrenSize = children.size();

    boolean splittable = false;
    int lastIndex = context.localVarIndex(context.newLocalVariable());      // The last index of split (i.e. pattern delimiter).

    // Start from offset if the first bit is a constant (corner case)
    if (childrenSize > 0 && children.get(0) instanceof StringLiteral)
      methodVisitor.visitLdcInsn(((StringLiteral) children.get(0)).unquotedValue().length());
    else
      methodVisitor.visitIntInsn(BIPUSH, -1);

    methodVisitor.visitIntInsn(ISTORE, lastIndex);

    for (int j = 0; j < childrenSize; j++) {
      Node child = children.get(j);

      if (child instanceof Variable) {
        if (j < childrenSize - 1) {
          context.localVarIndex(context.newLocalVariable((Variable) child));

          Node next = children.get(j + 1);
          if (next instanceof StringLiteral) {
            // If the next node is a string literal, then we must split this
            // string across occurrences of the given literal.
            int thisIndex = context.localVarIndex(context.newLocalVariable());

            methodVisitor.visitVarInsn(ALOAD, argIndex);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
            emit(next);

            // If this is the second or greater pattern matcher, seek from the last location.
            if (splittable) {
              methodVisitor.visitIntInsn(ILOAD, lastIndex);
              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf",
                  "(Ljava/lang/String;I)I");
            } else {
              methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf",
                  "(Ljava/lang/String;)I");
            }
            methodVisitor.visitIntInsn(ISTORE, thisIndex);

            methodVisitor.visitIntInsn(ILOAD, thisIndex);
            methodVisitor.visitIntInsn(BIPUSH, -1);
            methodVisitor.visitJumpInsn(IF_ICMPLE, endOfClause); // Jump out of this clause

            int matchedPieceVar = context.localVarIndex(context.newLocalVariable((Variable) child));

            methodVisitor.visitVarInsn(ALOAD, argIndex);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");

            methodVisitor.visitIntInsn(ILOAD, lastIndex);

            Label startFromLastIndex = new Label();
            Label startFromZeroIndex = new Label();
            methodVisitor.visitIntInsn(BIPUSH, -1);
            methodVisitor.visitJumpInsn(IF_ICMPNE, startFromLastIndex);

            // Either start from 0 or lastindex of split.
            methodVisitor.visitIntInsn(BIPUSH, 0);
            methodVisitor.visitJumpInsn(GOTO, startFromZeroIndex);
            methodVisitor.visitLabel(startFromLastIndex);
            methodVisitor.visitIntInsn(ILOAD, lastIndex);
            methodVisitor.visitLabel(startFromZeroIndex);

            methodVisitor.visitIntInsn(ILOAD, thisIndex);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring",
                "(II)Ljava/lang/String;");

            // Save this piece into our variable.
            methodVisitor.visitVarInsn(ASTORE, matchedPieceVar);

            // Advance the index by the length of this match.
            methodVisitor.visitLdcInsn(((StringLiteral) next).unquotedValue().length());
            methodVisitor.visitIntInsn(ILOAD, thisIndex);
            methodVisitor.visitInsn(IADD);

            methodVisitor.visitIntInsn(ISTORE, lastIndex);

            splittable = true;
          } else {
            methodVisitor.visitVarInsn(ALOAD, argIndex);
            int matchedPieceVar = context.localVarIndex(context.newLocalVariable((Variable) child));
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf",
                "(C)Ljava/lang/Character;");
            methodVisitor.visitVarInsn(ASTORE, matchedPieceVar);
          }
        } else {
          int matchedPieceVar = context.localVarIndex(context.newLocalVariable((Variable) child));
          int strLen = context.localVarIndex(RUNTIME_STR_LEN_PREFIX + argIndex);

//          methodVisitor.visitVarInsn(ALOAD, argIndex);
          methodVisitor.visitIntInsn(ILOAD, strLen);
          methodVisitor.visitIntInsn(BIPUSH, 1);

          Label restOfString = new Label();
          Label assignToPiece = new Label();

          methodVisitor.visitJumpInsn(IF_ICMPNE, restOfString);
          methodVisitor.visitLdcInsn("");
          methodVisitor.visitJumpInsn(GOTO, assignToPiece);
          methodVisitor.visitLabel(restOfString);

          methodVisitor.visitVarInsn(ALOAD, argIndex);
          methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");

          methodVisitor.visitIntInsn(ILOAD, lastIndex);
          methodVisitor.visitIntInsn(BIPUSH, -1);

          Label restOfStringFromI = new Label();
          Label reduceString = new Label();
          methodVisitor.visitJumpInsn(IF_ICMPLE, restOfStringFromI);
          methodVisitor.visitIntInsn(ILOAD, lastIndex);
          methodVisitor.visitJumpInsn(GOTO, reduceString);
          methodVisitor.visitLabel(restOfStringFromI);
          methodVisitor.visitLdcInsn(i);
          methodVisitor.visitLabel(reduceString);
          methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring",
              "(I)Ljava/lang/String;");

          methodVisitor.visitLabel(assignToPiece);
          methodVisitor.visitVarInsn(ASTORE, matchedPieceVar);
        }
        i++;
      }
    }
  }

  private void emitListStructurePatternRule(PatternRule rule,
                                            MethodVisitor methodVisitor,
                                            Context context,
                                            Label matchedClause,
                                            Label endOfClause,
                                            int argIndex) {
    ListStructurePattern listPattern = (ListStructurePattern) rule.patterns.get(argIndex);
    List<Node> children = listPattern.children();

    int runtimeListSizeVar = context.localVarIndex(RUNTIME_LIST_SIZE_VAR_PREFIX + argIndex);
    methodVisitor.visitIntInsn(ILOAD, runtimeListSizeVar);
    methodVisitor.visitIntInsn(BIPUSH, children.size());
    methodVisitor.visitJumpInsn(IF_ICMPNE, endOfClause);

    // Slice the list by terminals in the pattern list.
    for (int j = 0, childrenSize = children.size(); j < childrenSize; j++) {
      Node child = children.get(j);
      if (child instanceof Variable) {
        trackLineAndColumn(child);

        // Store into structure vars.
        int localVar = context.localVarIndex(context.newLocalVariable(((Variable) child)));

        methodVisitor.visitVarInsn(ALOAD, argIndex);
        methodVisitor.visitIntInsn(BIPUSH, j);      // We dont support matching >128 args ;)
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get",
            "(I)Ljava/lang/Object;");
        methodVisitor.visitVarInsn(ASTORE, localVar);
      }
    }

    methodVisitor.visitJumpInsn(GOTO, matchedClause);
  }

  private void emitListDestructuringPatternRule(PatternRule rule,
                                                MethodVisitor methodVisitor,
                                                Context context,
                                                Label endOfClause,
                                                int argIndex) {
    ListDestructuringPattern listPattern = (ListDestructuringPattern) rule.patterns.get(argIndex);
    Label noMatch = new Label();

    int runtimeListSizeVar = context.localVarIndex(RUNTIME_LIST_SIZE_VAR_PREFIX + argIndex);

    int size = listPattern.children().size();
    if (size == 0) {
      methodVisitor.visitIntInsn(ILOAD, runtimeListSizeVar);
//      methodVisitor.visitJumpInsn(IFEQ, matchedClause);
//      methodVisitor.visitJumpInsn(GOTO, endOfClause);
      methodVisitor.visitJumpInsn(IFNE, endOfClause);
    } else if (size == 1) {
      methodVisitor.visitIntInsn(ILOAD, runtimeListSizeVar);
      methodVisitor.visitIntInsn(BIPUSH, 1);
//      methodVisitor.visitJumpInsn(IFNE, matchedClause);
//      methodVisitor.visitJumpInsn(GOTO, endOfClause);
      methodVisitor.visitJumpInsn(IFNE, endOfClause);

    } else {
      // Slice the list by terminals in the pattern list.
      int i = 0;
      List<Node> children = listPattern.children();
      for (int j = 0, childrenSize = children.size(); j < childrenSize; j++) {
        Node child = children.get(j);
        if (child instanceof Variable) {
          trackLineAndColumn(child);
          int localVar = context.localVarIndex(context.newLocalVariable(((Variable) child)));

          if (j < childrenSize - 1) {
            methodVisitor.visitVarInsn(ALOAD, argIndex);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get",
                "(I)Ljava/lang/Object;");
            methodVisitor.visitVarInsn(ASTORE, localVar);
          } else {
            methodVisitor.visitIntInsn(ILOAD, runtimeListSizeVar);
            methodVisitor.visitIntInsn(BIPUSH, 1);
            Label storeEmptyList = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPEQ, storeEmptyList);

            // Otherwise store a slice of the list.
            methodVisitor.visitVarInsn(ALOAD, argIndex);
            methodVisitor.visitLdcInsn(i);
            methodVisitor.visitIntInsn(ILOAD, runtimeListSizeVar);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList",
                "(II)Ljava/util/List;");
            methodVisitor.visitVarInsn(ASTORE, localVar);

            Label endOfStoreEmptyList = new Label();
            methodVisitor.visitJumpInsn(GOTO, endOfStoreEmptyList);

            methodVisitor.visitLabel(storeEmptyList);
            methodVisitor.visitFieldInsn(GETSTATIC, "java/util/Collections", "EMPTY_LIST",
                "Ljava/util/List;");
            methodVisitor.visitVarInsn(ASTORE, localVar);
            methodVisitor.visitLabel(endOfStoreEmptyList);
//            methodVisitor.visitJumpInsn(GOTO, matchedClause);

          }

          i++;
        }
      }
    }

    methodVisitor.visitLabel(noMatch);
  }
}
