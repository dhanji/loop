package loop.confidence.classes;

import loop.Loop;
import loop.lang.LoopObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClassesConfidenceTest {
  @Test
  public final void reverseListPatternMatching() {
    Object run = Loop.run("test/loop/confidence/classes/classes_1.loop");
    assertTrue(run instanceof LoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");
    map.put("mass", 123);

    assertEquals(map, object);
  }
}
