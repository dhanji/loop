package loop;

import org.junit.Before;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public abstract class LoopTest {
  @Before
  public void tearDown() throws Exception {
    LoopClassLoader.reset();
  }
}
