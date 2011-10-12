package loop;

import loop.ast.*;
import loop.ast.script.ArgDeclList;
import loop.ast.script.FunctionDecl;
import loop.ast.script.Unit;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SuppressWarnings({"FieldCanBeLocal"}) class CodeWriter {
  private static final CompiledTemplate TEMPLATE = TemplateCompiler.compileTemplate(
      CodeWriter.class.getResourceAsStream("func.template"));

  private final StringBuilder out = new StringBuilder();

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
   */
  private final Map<Class<?>, Emitter> emitters = new HashMap<Class<?>, Emitter>();

  CodeWriter() {
    emitters.put(Call.class, callEmitter);
    emitters.put(Computation.class, computationEmitter);
    emitters.put(IntLiteral.class, intEmitter);
    emitters.put(Variable.class, variableEmitter);
    emitters.put(BinaryOp.class, binaryOpEmitter);
    emitters.put(StringLiteral.class, stringLiteralEmitter);
    emitters.put(Assignment.class, callEmitter);
    emitters.put(InlineMapDef.class, inlineMapEmitter);
    emitters.put(InlineListDef.class, inlineListEmitter);
    emitters.put(IndexIntoList.class, indexIntoListEmitter);
    emitters.put(CallChain.class, callChainEmitter);
    emitters.put(FunctionDecl.class, functionDeclEmitter);
    emitters.put(ArgDeclList.class, argDeclEmitter);
    emitters.put(PrivateField.class, privateFieldEmitter);
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
    if (!emitters.containsKey(node.getClass()))
      throw new RuntimeException("missing emitter for: " + node.getClass().getSimpleName());
    emitters.get(node.getClass()).emitCode(node);
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
      out.append("def ").append(normalizeMethodName(functionDecl.name()));
      emit(functionDecl.arguments());
      out.append(" {\n");
      emitChildren(node);
      out.append("\n}");
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

}
