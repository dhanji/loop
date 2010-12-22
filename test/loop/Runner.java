package loop;

/**
 * Use to run script files.
 */
public class Runner {
  public static void main(String...args) {
    CompilingInterpreter.execute("test/loop/scripts/math_test.loop");
  }
}
