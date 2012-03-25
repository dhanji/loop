package loop.confidence.classes;

import loop.Loop;
import loop.LoopTest;
import loop.lang.ImmutableLoopObject;
import loop.lang.LoopObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ClassesConfidenceTest extends LoopTest {
  @Test
  public final void simpleClassDeclAndInstantiation() {
    Object run = Loop.run("test/loop/confidence/classes/classes_1.loop");
    System.out.println(run);
    assertTrue(run instanceof LoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");
    map.put("mass", 123);

    assertEquals(map, object);
  }

  @Test
  public final void instantiateClassWithDefaults() {
    Object run = Loop.run("test/loop/confidence/classes/classes_2.loop");
    assertTrue(run instanceof LoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");
    map.put("mass", 123);

    assertEquals(map, object);
  }

  @Test
  public final void instantiateClassWithOneDefault() {
    Object run = Loop.run("test/loop/confidence/classes/classes_3.loop");
    assertTrue(run instanceof LoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");

    assertEquals(map, object);
  }

  @Test(expected = RuntimeException.class)
  public final void instantiateImmutableClassAndTryToMutate() {
    Object run = Loop.run("test/loop/confidence/classes/classes_4.loop");
    assertTrue(run instanceof ImmutableLoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");

    assertEquals(map, object);
  }

  @Test
  public final void instantiateImmutableClass() {
    Object run = Loop.run("test/loop/confidence/classes/classes_5.loop");
    assertTrue(run instanceof ImmutableLoopObject);

    LoopObject object = (LoopObject) run;
    assertEquals("Star", object.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");

    assertEquals(map, object);
  }

  @Test
  public final void instantiateAndImmutizeClass() {
    Object run = Loop.run("test/loop/confidence/classes/classes_6.loop");
    assertTrue(run instanceof List);

    @SuppressWarnings("unchecked")
    List<LoopObject> pair = (List<LoopObject>) run;

    LoopObject frozen = pair.get(0);
    LoopObject original = pair.get(1);
    assertTrue(frozen.equals(original));

    // Mutate original
    original.put("name", "Stuff");

    assertEquals("Star", frozen.getType().name);
    assertEquals("Star", original.getType().name);

    Map<Object, Object> map = new HashMap<Object, Object>();
    map.put("name", "Proxima Centauri");
    assertEquals(map, frozen);

    map = new HashMap<Object, Object>();
    map.put("name", "Stuff");
    assertEquals(map, original);

    // Attempt to mutate!
    Exception ex = null;
    try {
      frozen.put("name", original.get("name"));
      fail();
    } catch (RuntimeException e) {
      ex = e;
    }

    assertNotNull(ex);
  }
}
