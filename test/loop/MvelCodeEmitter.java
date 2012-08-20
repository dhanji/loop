package loop;

import loop.ast.*;
import loop.ast.TernaryIfExpression;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import loop.runtime.Closure;
import loop.runtime.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings({"FieldCanBeLocal"})
class MvelCodeEmitter {
  private static final AtomicInteger functionNameSequence = new AtomicInteger();
  private static final Map<String, String> BINARY_OP_TRANSLATIONS = new HashMap<String, String>();
  private static final String AND = " && ";

  static {
    BINARY_OP_TRANSLATIONS.put("not", "!=");
  }

  private StringBuilder out = new StringBuilder();
  private final Stack<Context> functionStack = new Stack<Context>();

  // MVEL line and column to map back to our Loop AST.
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

  /**
   * Call : 'emitCall', Computation : 'emitComputation', IntLiteral : 'emitLiteral', Variable :
   * 'emitVariable', BinaryOp : 'emitBinaryOp', StringLiteral : 'emitString', Assignment :
   * 'emitAssignment', InlineMapDef : 'emitMap', InlineListDef : 'emitList', IndexIntoList :
   * 'emitIndexInto', CallChain : 'emitCallChain', PatternRule : 'emitPatternRule',
   */
  private static final Map<Class<?>, Emitter> EMITTERS = new HashMap<Class<?>, Emitter>();

  MvelCodeEmitter(Scope scope) {
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
    EMITTERS.put(TernaryIfExpression.class, ternaryExpressionEmitter);
    EMITTERS.put(Comprehension.class, comprehensionEmitter);
    EMITTERS.put(ConstructorCall.class, constructorCallEmitter);
  }

  public String write(Unit unit) {
    for (FunctionDecl functionDecl : unit.functions()) {
      emit(functionDecl);
      append('\n');
      append('\n');
    }
    return out.toString();
  }

  public String write(Node node) {
    // We don't really emit classes.
    if (node instanceof ClassDecl)
      return "";

    emit(node);
    append(";\n");

    return out.toString();
  }

  private MvelCodeEmitter append(Object obj) {
    return append(obj.toString());
  }

  private MvelCodeEmitter append(String str) {
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

  private MvelCodeEmitter append(char c) {
    out.append(c);
    if (c == '\n') {
      line++;
      column = 0;
    } else
      column++;

    return this;
  }

  private MvelCodeEmitter append(int n) {
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
      TernaryIfExpression expression = (TernaryIfExpression) node;

      trackLineAndColumn(expression);
      // IF test
      append('(');
      emit(expression.children().get(0));

      append(" ? ");
      emit(expression.children().get(1));
      append(" : ");
      emit(expression.children().get(2));
      append(")\n");
    }
  };

  private final Emitter computationEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Computation computation = (Computation) node;
      trackLineAndColumn(computation);
      append('(');
      emitChildren(computation);
      append(')');
    }
  };

  private final Emitter callEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Call call = (Call) node;
      String name;

      trackLineAndColumn(call);
      // This is a special invocation so we emit it without the dot.
      if (Closure.CALL_FORM.equals(call.name())) {
        name = "";

        // Chew the previous dot in the call chain.
        out.deleteCharAt(out.length() - 1);
      } else
        name = normalizeMethodName(call.name());

      append(name);
      append('(');
      List<Node> children = call.args().children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        emit(children.get(i));

        if (i < childrenSize - 1)
          append(", ");
      }
      append(')');
    }
  };

  private final Emitter constructorCallEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      ConstructorCall call = (ConstructorCall) node;
      trackLineAndColumn(call);

      // Resolve a loop type internally. Note that this makes dynamic linking
      // of Loop types impossible, but we CAN link Java binaries dynamically.
      ClassDecl classDecl = scope.resolve(call.name, true);
      if (classDecl != null) {
        append("loop.lang.LoopClass.newInstance('").append(classDecl.name);

        // If arguments are not positional, reorder and emit them correctly.
        append("', ");

        Map<String, Node> fields = new HashMap<String, Node>();
        for (Node nodeAssign : classDecl.children()) {
          if (nodeAssign instanceof Assignment) {
            Assignment assignment = (Assignment) nodeAssign;
            fields.put(((Variable) assignment.lhs()).name, assignment.rhs());
          }
        }
        List<Node> children = call.args().children();
        if (!children.isEmpty() || !fields.isEmpty()) {
          append('[');

          // First emit named-args as overrides of defaults.
          for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            CallArguments.NamedArg arg = (CallArguments.NamedArg) children.get(i);

            append("'").append(arg.name).append("':");
            emit(arg.arg);

            fields.remove(arg.name);
            if (i < childrenSize - 1)
              append(", ");
          }

          // Now emit any remaining defaults.
          int i = 0, fieldSize = fields.size();
          for (Map.Entry<String, Node> field : fields.entrySet()) {
            append("'").append(field.getKey()).append("':");
            emit(field.getValue());

            if (i < fieldSize - 1)
              append(", ");

            i++;
          }

          append(']');
        } else
          append("null");
        append(')');

      } else {
        // Emit Java constructor call.
        append("new ");
        if (call.modulePart != null)
          append(call.modulePart);
        append(call.name);

        append('(');
        List<Node> children = call.args().children();
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
          emit(children.get(i));

          if (i < childrenSize - 1)
            append(", ");
        }
        append(')');
      }
    }
  };

  private final Emitter binaryOpEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BinaryOp binaryOp = (BinaryOp) node;
      String name = BINARY_OP_TRANSLATIONS.get(binaryOp.name());

      if (null == name)
        name = binaryOp.name();
      trackLineAndColumn(binaryOp);
      append(' ').append(name).append(' ');
      emitOnlyChild(binaryOp);
    }
  };

  private final Emitter assignmentEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Assignment assignment = (Assignment) node;
//      if (!(assignment.lhs() instanceof Variable))
//        throw new RuntimeException("Expected a variable on the LHS of assignment: "
//            + Parser.stringify(assignment));

      trackLineAndColumn(assignment);
      emit(assignment.lhs());
      append(" = ");
      emit(assignment.rhs());
      append(";\n");
    }
  };

  private final Emitter variableEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Variable var = (Variable) node;
      trackLineAndColumn(var);
      append(var.name);
    }
  };

  private static String normalizeMethodName(String name) {
    return name.replaceFirst("@", "__");
  }

  private final Emitter intEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      IntLiteral intLiteral = (IntLiteral) node;
      trackLineAndColumn(intLiteral);
      append(intLiteral.value);
    }
  };

  private final Emitter booleanEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      BooleanLiteral booleanLiteral = (BooleanLiteral) node;
      trackLineAndColumn(booleanLiteral);
      append(booleanLiteral.value);
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
        List<Node> parts = string.parts;
        for (int i = 0, partSize = parts.size(); i < partSize; i++) {
          Node part = parts.get(i);
          if (part instanceof StringLiteral)
            append('"').append(((StringLiteral) part).value).append('"');
          else
            emit(part);

          // Concatenate string expression.
          if (i < partSize - 1)
            append(" + ");
        }
      } else
        append(string.value);
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

      Context context = new Context(functionDecl);
      for (Node arg : functionDecl.arguments().children()) {
        context.arguments.add(((ArgDeclList.Argument) arg).name());
      }
      functionStack.push(context);

      append("def ").append(normalizeMethodName(name));
      emit(functionDecl.arguments());
      append(" {\n");
      trackLineAndColumn(functionDecl);

      // Emit locally-scoped helper functions and variables.
      for (Node helper : functionDecl.whereBlock()) {
        emit(helper);
      }

      // We only support stack traces for non-pattern functions right now.
      if (!functionDecl.patternMatching) {
        append("loop.runtime.Tracer.push('").append(functionDecl.name()).append("');\n");

        String retVal = "$_" + functionNameSequence.incrementAndGet();

        append(retVal).append(" = ");
        emitChildren(node);

        append(";\nloop.runtime.Tracer.pop();\n");
        append("return ").append(retVal).append(";\n");
      } else
        emitChildren(node);

      if (functionDecl.patternMatching) {
        // If we got this far, then none of the patterns were sufficient.
        append("loop.Loop.error(\"No pattern rules matched arguments: ");
        append(context.arguments);
        append("\");\n");
      }
      append("\n}");

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

      if (inlineListDef.isSet) {
        append("new java.util.HashSet(");
      }
      append('[');
      trackLineAndColumn(inlineListDef);
      final List<Node> children = inlineListDef.children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        emit(children.get(i));
        if (i < childrenSize - 1)
          append(", ");
      }
      append(']');

      if (inlineListDef.isSet)
        append(")");
    }
  };

  private final Emitter inlineMapEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      InlineMapDef inlineMapDef = (InlineMapDef) node;

      if (inlineMapDef.isTree)
        append("new java.util.TreeMap(");
      append('[');

      trackLineAndColumn(inlineMapDef);
      final List<Node> children = inlineMapDef.children();
      if (children.isEmpty())
        append(':');
      else
        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
          emit(children.get(i));
          if (i < childrenSize - 1) {

            // On every other node, emit a ','
            if (i % 2 != 0)
              append(", ");
            else
              append(": ");
          }
        }
      append(']');
      if (inlineMapDef.isTree)
        append(')');
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
      append('[');

      trackLineAndColumn(indexIntoList);
      emit(indexIntoList.from());
      append(']');
    }
  };

  private final Emitter comprehensionEmitter = new Emitter() {
    @Override
    public void emitCode(Node node) {
      Comprehension comprehension = (Comprehension) node;
      append(" (");
      trackLineAndColumn(comprehension);

      for (Node element : comprehension.projection()) {
        replaceVarInTree(element, comprehension.var(), "$");
        emit(element);
      }

      append(" in ");
      emit(comprehension.inList());

      Node filter = comprehension.filter();
      if (filter != null) {
        // Replace all occurrences of var in filter with $.
        replaceVarInTree(filter, comprehension.var(), "$");

        append(" if ");
        emit(filter);
      }
      append(") ");
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
    String lastIndex = newLocalVariable();      // The last index of split (i.e. pattern delimiter).
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
            String thisIndex = newLocalVariable();
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

  private String newLocalVariable() {
    return "$__" + functionNameSequence.incrementAndGet();
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
