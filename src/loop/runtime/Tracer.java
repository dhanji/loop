package loop.runtime;

import loop.Executable;
import org.mvel2.PropertyAccessException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Tracer {
  private static final ThreadLocal<Stack<String>> tracingStack = new ThreadLocal<Stack<String>>();

  public static void push(String element) {
    Stack<String> trace = tracingStack.get();
    if (trace == null) {
      tracingStack.set(trace = new Stack<String>());
    }

    trace.push(element);
  }

  public static void pop() {
    tracingStack.get().pop();
  }

  public static Stack<String> getStackTrace() {
    return tracingStack.get();
  }

  public static void complete() {
    tracingStack.remove();
  }

  public static void printCurrentTrace(Executable executable,
                                       PropertyAccessException e,
                                       PrintStream out) {
    out.println(e.getCause().getMessage());

    List<String> elements = new ArrayList<String>(tracingStack.get());
    Collections.reverse(elements);
    for (String element : elements) {
      out.print("  at ");
      out.print(element);
      out.println("()");
    }
  }
}
