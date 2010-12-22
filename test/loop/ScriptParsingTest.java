package loop;

import org.junit.Test;

import static loop.ParserTest.compare;

/**
 * Tests parsing of script-level elements
 */
public class ScriptParsingTest {

  @Test
  public final void require() {
    compare("require [a, b, c]", "require a.b.c\n");
    compare("require [a, b, c]", "require a.b.c  \n");
  }

  @Test(expected = RuntimeException.class)
  public final void requireMissingEol() {
    compare("require [a, b, c]", "require a.b.c");
  }

  @Test
  public final void module() {
    compare("module [a, b, c]", "module a.b.c\n");
    compare("module [a, b, c]", "module a.b.c  \n");
  }

  @Test(expected = RuntimeException.class)
  public final void moduleMissingEol() {
    compare("module [a, b, c]", "module a.b.c");
  }
}
