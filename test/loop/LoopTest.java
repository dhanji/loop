package loop;

import org.junit.After;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public abstract class LoopTest {
  @After
  public void tearDown() throws Exception {
    LoopClassLoader.reset();
  }
}
