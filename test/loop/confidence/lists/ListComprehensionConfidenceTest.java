package loop.confidence.lists;

import loop.Loop;
import org.junit.Test;
import org.mvel2.MVEL;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ListComprehensionConfidenceTest {
//  @Test
  public final void projectFunctionAcrossList() {
    assertEquals(Arrays.asList(), Loop.run("test/loop/confidence/lists/projection.loop", true));
  }

  @Test
  public final void test() {
    System.out.println(MVEL.eval("ls = [1,2,3];\n ($ in ls);", new HashMap()));
  }
}
