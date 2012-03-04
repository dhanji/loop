package loop;

import loop.ast.Assignment;
import loop.ast.BinaryOp;
import loop.ast.BooleanLiteral;
import loop.ast.Call;
import loop.ast.CallArguments;
import loop.ast.CallChain;
import loop.ast.ClassDecl;
import loop.ast.Comprehension;
import loop.ast.Computation;
import loop.ast.ConstructorCall;
import loop.ast.DestructuringPair;
import loop.ast.Guard;
import loop.ast.IndexIntoList;
import loop.ast.InlineListDef;
import loop.ast.InlineMapDef;
import loop.ast.IntLiteral;
import loop.ast.JavaLiteral;
import loop.ast.ListDestructuringPattern;
import loop.ast.ListStructurePattern;
import loop.ast.MapPattern;
import loop.ast.Node;
import loop.ast.OtherwiseGuard;
import loop.ast.PatternRule;
import loop.ast.PrivateField;
import loop.ast.RegexLiteral;
import loop.ast.StringLiteral;
import loop.ast.StringPattern;
import loop.ast.TernaryExpression;
import loop.ast.TypeLiteral;
import loop.ast.Variable;
import loop.ast.WildcardPattern;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.runtime.Scope;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings({"FieldCanBeLocal"}) class AsmCodeEmitter implements Opcodes {
  private static final AtomicInteger functionNameSequence = new AtomicInteger();
  private static final String AND = " && ";

  private StringBuilder out = new StringBuilder();
  private final Stack<Context> functionStack = new Stack<Context>();

  // Java line and column to map back to our Loop AST.
  private int line;
  private int column;
  private final TreeMap<SourceLocation, Node> emittedNodeMap = new TreeMap<SourceLocation, Node>();
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

  private static final Map<Class<?>, Emitter> EMITTERS = new HashMap<Class<?>, Emitter>();

  AsmCodeEmitter(Scope scope) {
    this.scope = scope;

    EMITTERS.put(Call.class, callEmitter);
    EMITTERS.put(Computation.class, computationEmitter);
    EMITTERS.put(IntLiteral.class, intEmitter);
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
    EMITTERS.put(TernaryExpression.class, ternaryExpressionEmitter);
    EMITTERS.put(Comprehension.class, comprehensionEmitter);
    EMITTERS.put(ConstructorCall.class, constructorCallEmitter);
  }

  private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
  private final Stack<MethodVisitor> methodStack = new Stack<MethodVisitor>();

  public Class<?> write(Unit unit) {
    return write(unit, false);
  }

  public Class<?> write(Unit unit, boolean print) {
    Thread.currentThread().setContextClassLoader(LoopClassLoader.CLASS_LOADER);

    // We always emit functions as static into a containing Java class.
    String javaClass = unit.name();
    classWriter.visit(V1_6, ACC_PUBLIC, javaClass, null, "java/lang/Object", new String[0]);

    for (FunctionDecl functionDecl : unit.functions()) {
      emit(functionDecl);
      append('\n');
      append('\n');
    }

    classWriter.visitEnd();

    if (print) {
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

  public String write(Node node) {
    // We don't really emit classes.
    if (node instanceof ClassDecl)
      return "";

    emit(node);
    append(";\n");

    return out.toString();
  }

  private AsmCodeEmitter append(String str) {
    if (null == str)
      return this;
    out.append(str);
    if (str.contains("\n")) {
      line++;
      column = 0;
    } else
      column += str.length();

    return this;
  }

  private AsmCodeEmitter append(char c) {
    out.append(c);
    if (c == '\n') {
      line++;
      column = 0;
    } else
      column++;

    return this;
  }

  private AsmCodeEmitter append(int n) {
    out.append(n);
    column++;
    return this;
  }

  private void trackLineAndColumn(Node node) {
    emittedNodeMap.put(new SourceLocation(line, column), node);
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

  public void emitTo(Node node, StringBuilder alt) {
    StringBuilder tmp = out;
    out = alt;
    emit(node);
    out = tmp;
  }

  public TreeMap<SourceLocation, Node> getEmittedNodeMap() {
    return emittedNodeMap;
  }

  // -------------------------------------------------------------------
  // EMITTERS ----------------------------------------------------------
  // -------------------------------------------------------------------

  private final Emitter ternaryExpressionEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      TernaryExpression expression = (TernaryExpression) node;
      MethodVisitor methodVisitor = methodStack.peek();

      Label elseBranch = new Label();
      Label end = new Label();

      // If condition
      emit(expression.children().get(0));
      methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
      methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
      methodVisitor.visitJumpInsn(IFEQ, elseBranch);

      emit(expression.children().get(1));
      methodVisitor.visitJumpInsn(GOTO, end);

      methodVisitor.visitLabel(elseBranch);
      emit(expression.children().get(2));
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

  private final Emitter callEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Call call = (Call) node;
      String name;

      trackLineAndColumn(call);
      // This is a special invocation so we emit it without the dot.
      if ("@call".equals(call.name())) {
        name = "";

        // Chew the previous dot in the call chain.
        out.deleteCharAt(out.length() - 1);
      } else
        name = normalizeMethodName(call.name());

      if (!call.isFunction)
        name = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);

      MethodVisitor methodVisitor = methodStack.peek();

      boolean isStatic = scope.resolveFunction(call.name()) != null;
      // push name of containing type if this is a static call.
      if (isStatic)
        methodVisitor.visitLdcInsn("_default_");

      // push method name onto stack
      methodVisitor.visitLdcInsn(name);

      if (call.args() != null && !call.args().children().isEmpty()) {
        int arrayIndex = call.args().children().size();
        // push args as array.
        methodVisitor.visitIntInsn(BIPUSH, arrayIndex);       // size of array
        methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        methodVisitor.visitVarInsn(ASTORE, arrayIndex);
        if (!call.args().children().isEmpty()) {
          int i = 0;
          for (Node arg : call.args().children()) {
            methodVisitor.visitVarInsn(ALOAD, arrayIndex);    // array
            methodVisitor.visitIntInsn(BIPUSH, i);            // index
            emit(arg);                                        // value

            methodVisitor.visitInsn(AASTORE);
            i++;
          }
        }

        // Load the array back in.
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);

        if (isStatic) {
          // If JDK7, use invokedynamic instead for better performance.
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
              "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
          // If JDK7, use invokedynamic instead for better performance.
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "call",
              "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        }
      } else {
        if (isStatic) {
          // If JDK7, use invokedynamic instead for better performance.
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Caller", "callStatic",
              "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
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

      // Resolve a loop type internally. Note that this makes dynamic linking
      // of Loop types impossible, but we CAN link Java binaries dynamically.
      ClassDecl classDecl = scope.resolve(call.name);
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
          }

          // Now emit any remaining defaults.
          for (Map.Entry<String, Node> field : fields.entrySet()) {
            methodVisitor.visitVarInsn(ALOAD, objectVar);
            methodVisitor.visitLdcInsn(field.getKey());
            emit(field.getValue());
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
          }
        }

        // Leave the object on the stack.
        methodVisitor.visitVarInsn(ALOAD, objectVar);

      } else {
        // Emit Java constructor call.
        String javaType = (call.modulePart != null ? call.modulePart : "") + call.name;

        boolean isNullary = call.args().children().isEmpty();
        if (!isNullary) {
          int arrayIndex = context.localVarIndex(context.newLocalVariable());
          methodVisitor.visitIntInsn(BIPUSH, arrayIndex);       // size of array
          methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
          methodVisitor.visitVarInsn(ASTORE, arrayIndex);

          int i = 0;
          for (Node arg : call.args().children()) {
            methodVisitor.visitVarInsn(ALOAD, arrayIndex);    // array
            methodVisitor.visitIntInsn(BIPUSH, i);            // index
            emit(arg);                                        // value

            methodVisitor.visitInsn(AASTORE);
            i++;
          }

          // push type and constructor arg array.
          methodVisitor.visitLdcInsn(javaType);
          methodVisitor.visitVarInsn(ALOAD, arrayIndex);

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
          methodVisitor.visitInsn(IAND);
          break;
        case OR:
          methodVisitor.visitInsn(IOR);
          break;
        case NOT:
          methodVisitor.visitInsn(INEG);
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
      emit(assignment.lhs());

      emit(assignment.rhs());
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

  private final Emitter booleanEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BooleanLiteral booleanLiteral = (BooleanLiteral) node;

      // Emit int wrappers.
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
      append(type.name);
    }
  };

  private final Emitter javaLiteralEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      JavaLiteral java = (JavaLiteral) node;
      trackLineAndColumn(java);
      append(java.value);
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
      RegexLiteral regex = (RegexLiteral) node;
      append('"').append(regex.value).append('"');
    }
  };

  private final Emitter functionDeclEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      FunctionDecl functionDecl = (FunctionDecl) node;
      String name = functionDecl.name();
      if (name == null) {
        // Function is anonymous, generate a globally unique name for it.
        name = "$" + functionNameSequence.incrementAndGet();
      }

      Context context = new Context(name);
      StringBuilder args = new StringBuilder("(");
      List<Node> children = functionDecl.arguments().children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        Node arg = children.get(i);
        String argName = ((ArgDeclList.Argument) arg).name();
        context.arguments.add(argName);
        context.argumentIndex.put(argName, i);

        args.append("Ljava/lang/Object;");
      }
      args.append(")");
      functionStack.push(context);

      final MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC + ACC_STATIC,
          normalizeMethodName(name),
          args.append("Ljava/lang/Object;").toString(),
          null,
          null);
      methodStack.push(methodVisitor);

      // Emit locally-scoped helper functions and variables.
      for (Node helper : functionDecl.whereBlock) {
        emit(helper);
      }

      emitChildren(node);

      methodVisitor.visitInsn(ARETURN);
      methodVisitor.visitMaxs(0, 0);
      methodVisitor.visitEnd();

      methodStack.pop();
      functionStack.pop();
    }
  };

  private final Emitter privateFieldEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      PrivateField privateField = (PrivateField) node;
      trackLineAndColumn(privateField);
      append(normalizeMethodName(privateField.name()));
    }
  };

  private final Emitter argDeclEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      ArgDeclList argDeclList = (ArgDeclList) node;
      append('(');

      trackLineAndColumn(argDeclList);
      List<Node> children = argDeclList.children();

      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        Node child = children.get(i);
        ArgDeclList.Argument arg = (ArgDeclList.Argument) child;
        append(arg.name());
        if (i < childrenSize - 1)
          append(", ");
      }
      append(')');
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
          append('?');

        trackLineAndColumn(child);
        emit(child);

        if (i < childrenSize - 1) {
          // Do not emit DOT if the next node is not a method or property.
          Node next = children.get(i + 1);
          if (next instanceof IndexIntoList)
            continue;

          if (i == childrenSize - 2 || !callChain.nullSafe)
            append('.');
          else
            append(".?");
        }
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
              "(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Object;");
        else
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "obtain",
              "(Ljava/lang/Object;Ljava/lang/Integer;Ljava/lang/Integer;)Ljava/lang/Object;");
      } else {
        if (indexIntoList.isSlice())
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "sliceFrom",
              "(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Object;");
        else
          methodVisitor.visitMethodInsn(INVOKESTATIC, "loop/runtime/Collections", "obtain",
              "(Ljava/lang/Object;Ljava/lang/Integer;)Ljava/lang/Object;");
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
      // out.add(iterator.next())
      methodVisitor.visitVarInsn(ALOAD, outVarIndex);
      methodVisitor.visitVarInsn(ALOAD, nextIndex);
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

      if (context.arguments.isEmpty())
        throw new RuntimeException("Incorrect number of arguments for pattern matching");

      if (context.arguments.size() != rule.patterns.size())
        throw new RuntimeException("Incorrect number of pattern rules. Expected pattern rules for "
            + context.arguments + " but found " + rule.patterns.size() + " rule(s): "
            + Parser.stringify(rule.patterns));

      List<EmittedWrapping> emitIntoBody = new ArrayList<EmittedWrapping>();
      int mark = out.length(), emittedArgs = 0;
      append("if (");
      for (int i = 0, argumentsSize = context.arguments.size(); i < argumentsSize; i++) {

        boolean wasArgumentEmitted = true;
        emittedArgs++;
        Node pattern = rule.patterns.get(i);
        if (pattern instanceof ListDestructuringPattern) {
          emitIntoBody.add(emitListDestructuringPatternRule(rule, context, i));
        } else if (pattern instanceof ListStructurePattern) {
          emitIntoBody.add(emitListStructurePatternRule(rule, context, i));
        } else if (pattern instanceof StringLiteral
            || pattern instanceof IntLiteral) {
          String argument = context.arguments.get(i);
          append(argument).append(" == ");

          emit(pattern);
        } else if (pattern instanceof RegexLiteral) {
          String argument = context.arguments.get(i);
          append(argument).append(" ~= ");
          emit(pattern);
        } else if (pattern instanceof StringPattern) {
          emitIntoBody.add(emitStringPatternRule(rule, context, i));
        } else if (pattern instanceof MapPattern) {
          emitIntoBody.add(emitMapPatternRule(rule, context, i));
        } else if (pattern instanceof WildcardPattern) {

          // If this is the last argument, then we don't need the preceding &&.
          if (i == argumentsSize - 1) {
            if (AND.equals(out.substring(out.length() - AND.length())))
              out.delete(out.length() - AND.length(), out.length());
          }

          wasArgumentEmitted = false;
          emittedArgs--;
        }

        if (wasArgumentEmitted && i < context.arguments.size() - 1)
          append(AND);
      }

      if (emittedArgs == 0) {
        out.delete(mark, mark + "if (".length());
      } else
        append(") {\n ");

      for (EmittedWrapping emittedWrapping : emitIntoBody) {
        if (null != emittedWrapping)
          append(emittedWrapping.inbody);
      }

      emitPatternClauses(rule);

      for (EmittedWrapping emittedWrapping : emitIntoBody) {
        if (null != emittedWrapping)
          append(emittedWrapping.after);
      }

      if (emittedArgs == 0)
        append(";\n");
      else
        append(";\n}\n");
    }
  };

  private void emitPatternClauses(PatternRule rule) {
    if (rule.rhs != null) {
      append(" return ");
      emit(rule.rhs);
    } else
      emitGuards(rule);
  }

  private void emitGuards(PatternRule rule) {
    List<Node> children = rule.children();
    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
      Node node = children.get(i);
      if (!(node instanceof Guard))
        throw new RuntimeException("Apparent pattern rule missing guards: "
            + Parser.stringify(rule));

      if (i > 0)
        append(" else ");

      Guard guard = (Guard) node;

      // The "Otherwise" expression is a plain else.
      if (!(guard.expression instanceof OtherwiseGuard)) {
        append("if (");
        emit(guard.expression);
        append(") ");
      }
      append("{\n return ");
      emit(guard.line);

      // If this is not the last guard.
      append(";\n} ");
    }
  }

  private EmittedWrapping emitMapPatternRule(PatternRule rule, Context context, int argIndex) {
    String argument = context.arguments.get(argIndex);
    MapPattern pattern = (MapPattern) rule.patterns.get(argIndex);

    StringBuilder inbody = new StringBuilder();
    List<Node> children = pattern.children();
    for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
      Node destructuring = children.get(i);
      DestructuringPair pair = (DestructuringPair) destructuring;
      append(argument).append(".?");
      emit(pair.rhs);

      append(" != null");
      if (i < childrenSize - 1)
        append(" && ");

      emitTo(pair.lhs, inbody);
      inbody.append(" = ").append(argument).append('.');
      emitTo(pair.rhs, inbody);
      inbody.append(";\n");
    }
    return new EmittedWrapping(inbody.toString(), null);
  }

  private EmittedWrapping emitStringPatternRule(PatternRule rule, Context context, int argIndex) {
    String argument = context.arguments.get(argIndex);
    append(argument).append(" is String");
    List<Node> children = rule.patterns.get(argIndex).children();
    int i = 0, childrenSize = children.size();

    StringBuilder inbody = new StringBuilder();
    boolean splittable = false;
    String lastIndex =
        context.newLocalVariable();      // The last index of split (i.e. pattern delimiter).
    inbody.append(lastIndex).append(" = -1;\n");

    int ifCount = 0;
    for (int j = 0; j < childrenSize; j++) {
      Node child = children.get(j);

      if (child instanceof Variable) {
        if (j < childrenSize - 1) {

          Node next = children.get(j + 1);
          if (next instanceof StringLiteral) {
            // If the next node is a string literal, then we must split this
            // string across occurrences of the given literal.
            String thisIndex = context.newLocalVariable();
            inbody.append(thisIndex).append(" = ");
            inbody.append(argument).append(".indexOf(");
            emitTo(next, inbody);

            // If this is the second or greater pattern matcher, seek from the last location.
            if (splittable) {
              inbody.append(", ").append(lastIndex);
            }

            inbody.append(");\n");
            inbody.append("if (").append(thisIndex).append(" > -1) {\n");
            emitTo(child, inbody);
            inbody.append(" = ")
                .append(argument)
                .append(".substring(")
                .append(lastIndex)
                .append(" == -1 ? 0 : ")
                .append(lastIndex)
                .append(", ")
                .append(thisIndex)
                .append(");\n");
            // Advance the index by the length of this match.
            inbody.append(lastIndex).append(" = ").append(thisIndex).append(" + ");
            emitTo(next, inbody);
            inbody.append(".length();\n");

            ifCount++;
            splittable = true;
          } else {
            emitTo(child, inbody);
            inbody.append(" = ").append(argument).append(".charAt(").append(i).append(");\n");
          }
        } else {
          emitTo(child, inbody);
          inbody.append(" = ").append(argument).append(".length() == 1 ? '' : ").append(argument);
          inbody.append(".substring(").append(lastIndex).append(" > -1 ? ")
              .append(lastIndex).append(": ").append(i).append(");\n");
        }
        i++;
      }
    }

    // Close If statements in reverse order.
    for (int j = 0; j < ifCount; j++) {
      inbody.append("} else {\n ").append(lastIndex).append(" = -1\n }\n");
    }

    // Only process the return rule if patterns matched.
    if (splittable) {
      inbody.append("if (").append(lastIndex).append(" > -1) {\n");
    }

    return new EmittedWrapping(inbody.toString(), splittable ? "\n}\n" : null);
  }

  private EmittedWrapping emitListStructurePatternRule(PatternRule rule,
                                                       Context context,
                                                       int argIndex) {
    ListStructurePattern listPattern = (ListStructurePattern) rule.patterns.get(argIndex);
    String arg0 = context.arguments.get(argIndex);
    append(arg0);
    append(" is java.util.List");

    if (listPattern.children().size() > 0)
      append(" && ").append(arg0).append(".size() == ").append(listPattern.children().size());

    // Slice the list by terminals in the pattern list.
    List<Node> children = listPattern.children();
    StringBuilder inbody = new StringBuilder();
    for (int j = 0, childrenSize = children.size(); j < childrenSize; j++) {
      Node child = children.get(j);
      if (child instanceof Variable) {
        trackLineAndColumn(child);
        inbody.append(((Variable) child).name);

        inbody.append(" = ");
        inbody.append(arg0);
        inbody.append('[').append(j).append("];\n");
      }
    }

    return new EmittedWrapping(inbody.toString(), null);
  }

  private EmittedWrapping emitListDestructuringPatternRule(PatternRule rule,
                                                           Context context,
                                                           int argIndex) {
    ListDestructuringPattern listPattern = (ListDestructuringPattern) rule.patterns.get(argIndex);
    String arg0 = context.arguments.get(argIndex);
    append(arg0);
    append(" is java.util.List");

    int size = listPattern.children().size();
    if (size == 0) {
      append(" && (");
      append(arg0);
      append(" == empty) ");
    } else if (size == 1) {
      append(" && (");
      append(arg0);
      append(".size() == 1) ");
    } else {
      // Slice the list by terminals in the pattern list.
      int i = 0;
      StringBuilder inbody = new StringBuilder();
      List<Node> children = listPattern.children();
      for (int j = 0, childrenSize = children.size(); j < childrenSize; j++) {
        Node child = children.get(j);
        if (child instanceof Variable) {
          trackLineAndColumn(child);
          inbody.append(((Variable) child).name);
          inbody.append(" = ");
          inbody.append(arg0);

          if (j < childrenSize - 1)
            inbody.append('[').append(i).append("];\n");
          else {
            inbody.append(".size() == 1 ? [] : ").append(arg0);
            inbody.append(".subList(").append(i).append(',').append(arg0).append(".size());\n");
          }
          i++;
        }
      }

      return new EmittedWrapping(inbody.toString(), null);
    }
    return null;
  }
}
