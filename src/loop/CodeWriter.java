package loop;

import loop.ast.Assignment;
import loop.ast.BinaryOp;
import loop.ast.Call;
import loop.ast.CallChain;
import loop.ast.Computation;
import loop.ast.IndexIntoList;
import loop.ast.InlineListDef;
import loop.ast.InlineMapDef;
import loop.ast.IntLiteral;
import loop.ast.ListPattern;
import loop.ast.Node;
import loop.ast.OtherwisePattern;
import loop.ast.PatternRule;
import loop.ast.PrivateField;
import loop.ast.StringLiteral;
import loop.ast.StringPattern;
import loop.ast.Variable;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings({"FieldCanBeLocal"}) class CodeWriter {
  private static final AtomicInteger functionNameSequence = new AtomicInteger();

  private final StringBuilder out = new StringBuilder();
  private final Stack<Context> functionStack = new Stack<Context>();

  private static interface Emitter {
    void emitCode(Node node);
  }

  /**
   * Call : 'emitCall',
   * Computation : 'emitComputation',
   * IntLiteral : 'emitLiteral',
   * Variable : 'emitVariable',
   * BinaryOp : 'emitBinaryOp',
   * StringLiteral : 'emitString',
   * Assignment : 'emitAssignment',
   * InlineMapDef : 'emitMap',
   * InlineListDef : 'emitList',
   * IndexIntoList : 'emitIndexInto',
   * CallChain : 'emitCallChain',
   * PatternRule : 'emitPatternRule',
   */
  private static final Map<Class<?>, Emitter> EMITTERS = new HashMap<Class<?>, Emitter>();

  CodeWriter() {
    EMITTERS.put(Call.class, callEmitter);
    EMITTERS.put(Computation.class, computationEmitter);
    EMITTERS.put(IntLiteral.class, intEmitter);
    EMITTERS.put(Variable.class, variableEmitter);
    EMITTERS.put(BinaryOp.class, binaryOpEmitter);
    EMITTERS.put(StringLiteral.class, stringLiteralEmitter);
    EMITTERS.put(Assignment.class, callEmitter);
    EMITTERS.put(InlineMapDef.class, inlineMapEmitter);
    EMITTERS.put(InlineListDef.class, inlineListEmitter);
    EMITTERS.put(IndexIntoList.class, indexIntoListEmitter);
    EMITTERS.put(CallChain.class, callChainEmitter);
    EMITTERS.put(FunctionDecl.class, functionDeclEmitter);
    EMITTERS.put(ArgDeclList.class, argDeclEmitter);
    EMITTERS.put(PrivateField.class, privateFieldEmitter);
    EMITTERS.put(PatternRule.class, patternRuleEmitter);
  }

  public String write(Unit unit) {
    for (FunctionDecl functionDecl : unit.functions()) {
      emit(functionDecl);
      out.append('\n');
      out.append('\n');
    }
    return out.toString();
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

  private final Emitter computationEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      Computation computation = (Computation) node;
      out.append('(');
      emitChildren(computation);
      out.append(')');
    }
  };

  private final Emitter callEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      Call call = (Call) node;
      out.append(normalizeMethodName(call.name())).append('(');
      if (!call.args().children().isEmpty()) {
        emit(call.args().children().get(0));
      }
      out.append(')');
    }
  };

  private final Emitter binaryOpEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      BinaryOp binaryOp = (BinaryOp) node;
      out.append(binaryOp.name()).append(' ');
      emitOnlyChild(binaryOp);
    }
  };

  private final Emitter variableEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      Variable var = (Variable) node;
      out.append(var.name);
    }
  };

  private static String normalizeMethodName(String name) {
    return name.replace("@", "__");
  }

  private final Emitter intEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      IntLiteral intLiteral = (IntLiteral) node;
      out.append(intLiteral.value);
    }
  };

  private final Emitter stringLiteralEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      StringLiteral string = (StringLiteral) node;
      out.append(string.value);
    }
  };

  private final Emitter functionDeclEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      FunctionDecl functionDecl = (FunctionDecl) node;
      String name = functionDecl.name();
      if (name == null) {
        // Function is anonymous, generate a globally unique name for it.
        name = "$" + functionNameSequence.incrementAndGet();
      }

      // Emit locally-scoped helper functions.
      for (FunctionDecl helper : functionDecl.whereBlock) {
        emit(helper);
      }

      Context context = new Context(name);
      for (Node arg : functionDecl.arguments().children()) {
        context.arguments.add(((ArgDeclList.Argument)arg).name());
      }
      functionStack.push(context);

      out.append("def ").append(normalizeMethodName(name));
      emit(functionDecl.arguments());
      out.append(" {\n");
      emitChildren(node);
      if (functionDecl.patternMatching) {
        // If we got this far, then none of the patterns were sufficient.
        out.append("loop.Loop.error(\"No pattern rules matched arguments: ");
        out.append(context.arguments);
        out.append("\");\n");
      }
      out.append("\n}");

      functionStack.pop();
    }
  };

  private final Emitter privateFieldEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      PrivateField privateField = (PrivateField) node;
      out.append(normalizeMethodName(privateField.name()));
    }
  };

  private final Emitter argDeclEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      ArgDeclList argDeclList = (ArgDeclList) node;
      out.append('(');
      List<Node> children = argDeclList.children();

      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        Node child = children.get(i);
        ArgDeclList.Argument arg = (ArgDeclList.Argument) child;
        out.append(arg.name());
        if (i < childrenSize - 1)
          out.append(", ");
      }
      out.append(')');
    }
  };

  private final Emitter inlineListEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      InlineListDef inlineListDef = (InlineListDef) node;

      out.append('[');
      final List<Node> children = inlineListDef.children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        emit(children.get(i));
        if (i < childrenSize - 1)
          out.append(", ");
      }
      out.append(']');
    }
  };

  private final Emitter inlineMapEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      InlineMapDef inlineMapDef = (InlineMapDef) node;

      out.append('[');
      final List<Node> children = inlineMapDef.children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        emit(children.get(i));
        if (i < childrenSize - 1) {

          // On every other node, emit a ','
          if (i % 2 != 0)
            out.append(", ");
          else
            out.append(": ");
        }
      }
      out.append(']');
    }
  };


  private final Emitter callChainEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      CallChain callChain = (CallChain) node;
      List<Node> children = callChain.children();
      for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
        Node child = children.get(i);
        emit(child);

        if (i < childrenSize - 1) {
          // Do not emit DOT if the next node is not a method or property.
          if (children.get(i + 1) instanceof IndexIntoList)
            continue;
          out.append('.');
        }
      }
    }
  };

  private final Emitter indexIntoListEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      IndexIntoList indexIntoList = (IndexIntoList) node;
      out.append('[');
      emit(indexIntoList.from());
      out.append(']');
    }
  };

  private final Emitter patternRuleEmitter = new Emitter() {
    @Override public void emitCode(Node node) {
      // TODO for now we only handle one argument.
      PatternRule rule = (PatternRule) node;
      Context context = functionStack.peek();

      if (context.arguments.isEmpty())
        throw new RuntimeException("Incorrect number of arguments for pattern matching");

      if (rule.pattern instanceof ListPattern) {
        emitListPatternRule(rule, context);
      } else if (rule.pattern instanceof StringLiteral
          || rule.pattern instanceof IntLiteral) {
        String arg0 = context.arguments.get(0);
        out.append("if (").append(arg0).append(" == ");
        emit(rule.pattern);
        out.append(") {\n return ");
        emit(rule.rhs);
        out.append(";\n}\n");
      } else if (rule.pattern instanceof StringPattern) {
        emitStringPatternRule(rule, context);
      } else if (rule.pattern instanceof OtherwisePattern) {
        out.append("return ");
        emit(rule.rhs);
        out.append(";\n");
      }
    }
  };

  private void emitStringPatternRule(PatternRule rule, Context context) {
    String arg0 = context.arguments.get(0);
    out.append("if (").append(arg0).append(" is String) {\n");
    List<Node> children = rule.pattern.children();
    int i = 0, childrenSize = children.size();

    boolean splittable = false;
    String lastIndex = newLocalVariable();      // The last index of split (i.e. pattern delimiter).
    out.append(lastIndex).append(" = -1;\n");

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
            out.append(thisIndex).append(" = ");
            out.append(arg0).append(".indexOf(");
            emit(next);

            // If this is the second or greater pattern matcher, seek from the last location.
            if (splittable) {
              out.append(", ").append(lastIndex);
            }

            out.append(");\n");
            out.append("if (").append(thisIndex).append(" > -1) {\n");
            emit(child);
            out.append(" = ").append(arg0).append(".substring(").append(lastIndex).append(" == -1 ? 0 : ").append(lastIndex)
                .append(", ").append(thisIndex).append(");\n");
            // Advance the index by the length of this match.
            out.append(lastIndex).append(" = ").append(thisIndex).append(" + ");
            emit(next);
            out.append(".length();\n");

            ifCount++;
            splittable = true;
          } else {
            emit(child);
            out.append(" = ").append(arg0).append(".charAt(").append(i).append(");\n");
          }
        } else {
          emit(child);
          out.append(" = ").append(arg0).append(".length() == 1 ? '' : ").append(arg0);
          out.append(".substring(").append(lastIndex).append(" > -1 ? ")
              .append(lastIndex).append(": ").append(i).append(");\n");
        }
        i++;
      }
    }

    // Close If statements in reverse order.
    for (int j = 0; j < ifCount; j++) {
      out.append("} else { ").append(lastIndex).append(" = -1\n }\n");
    }

    // Only process the return rule if patterns matched.
    if (splittable) {
      out.append("if (").append(lastIndex).append(" > -1) {\n");
    }
    out.append("return ");
    emit(rule.rhs);
    out.append(';');
    if (splittable) {
      out.append("\n}\n");
    }
    out.append("\n}\n");
  }

  private String newLocalVariable() {
    return "$__" + functionNameSequence.incrementAndGet();
  }

  private void emitListPatternRule(PatternRule rule, Context context) {
    ListPattern listPattern = (ListPattern) rule.pattern;
    String arg0 = context.arguments.get(0);
    out.append("if (");
    out.append(arg0);
    out.append(" is java.util.List) {\n");

    int size = listPattern.children().size();
    if (size == 0) {
      out.append("if (");
      out.append(arg0);
      out.append(" == empty) {\n return ");
      emit(rule.rhs);
      out.append(";\n}\n");
    } else if (size == 1) {
      out.append("if (");
      out.append(arg0);
      out.append(".size() == 1) {\n return ");
      emit(rule.rhs);
      out.append(";\n}\n");
    } else {
      // Slice the list by terminals in the pattern list.
      int i = 0;
      List<Node> children = listPattern.children();
      for (int j = 0, childrenSize = children.size(); j < childrenSize; j++) {
        Node child = children.get(j);
        if (child instanceof Variable) {
          emit(child);
          out.append(" = ");
          out.append(arg0);

          if (j < childrenSize - 1)
            out.append('[').append(i).append("];\n");
          else {
            out.append(".size() == 1 ? [] : ").append(arg0);
            out.append(".subList(").append(i).append(',').append(arg0).append(".size());\n");
          }
          i++;
        }
      }

      out.append("return ");
      emit(rule.rhs);
      out.append(';');
    }
    out.append("}\n");
  }

}
