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
    compare("require [a, b, c] as x", "require a.b.c as x  \n");
  }

  @Test
  public final void requireJavaLiteral() {
    compare("require `java.util.List`", "require `java.util.List`\n");
    compare("require `java.sql.Date`", "require `java.sql.Date`  \n");
  }

  @Test(expected = LoopCompileException.class)
  public final void requireJavaLiteralCannotBeAliased() {
    compare("require `java.util.List`", "require `java.util.List`\n");
    compare("require `java.sql.Date`", "require `java.sql.Date` as d \n");
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
