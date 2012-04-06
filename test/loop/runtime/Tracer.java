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

  public static void startTrace() {
    if (tracingStack.get() == null)
      tracingStack.set(new Stack<String>());
  }

  public static void push(String element) {
//    tracingStack.get().push(element);
  }

  public static void pop() {
//    tracingStack.get().pop();
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
    Stack<String> stackTrace = tracingStack.get();
    if (stackTrace.isEmpty())
      return;

    out.println("trace:");
    List<String> elements = new ArrayList<String>(stackTrace);
    Collections.reverse(elements);
    for (String element : elements) {
      out.print("  at ");
      out.print(element);
      out.println("()");

      // Determine line and column of function.
    }
  }
}
