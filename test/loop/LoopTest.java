package loop;

import loop.ast.script.ModuleLoader;
import org.junit.Before;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public abstract class LoopTest {
  @Before
  public void tearDown() throws Exception {
    LoopClassLoader.reset();

    // Reset the module search path.
    ModuleLoader.reset();
  }
}
