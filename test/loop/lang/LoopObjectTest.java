package loop.lang;

import loop.LoopExecutionException;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopObjectTest {

  private LoopClass type;
  private LoopObject object;
  private LoopObject object2;

  @Before
  public final void pre() {
    type = new LoopClass("type1");
    object = new LoopObject(type);

    object2 = new LoopObject(type);
    object2.put("sodaosk", "aosdo");

    object.put("stuff", "buff");
    object.put("stuff2", 1);
    object.put(18273L, new BigDecimal("123124"));
    object.put("obj", object2);
    List<Integer> integers = Arrays.asList(1, 2, 3);
    object.put("ls", integers);
  }

  @Test
  public final void mutableCopyOfMutableObject() {
    LoopObject copy = object.copy();

    assertNotSame(copy, object);
    assertEquals(copy, object);

    copy.put("ad", "123");

    assertFalse(copy.equals(object));
  }

  @Test @SuppressWarnings("unchecked")
  public final void immutableCopyOfImmutableObject() {
    object = new ImmutableLoopObject(type, object);

    object2.put(844, "aosdo");

    LoopObject copy = object.immutize();
    assertNotSame(object, copy);
    assertEquals(object, copy);

    Map obj = (Map) object.get("obj");
    assertNull(obj.get(844));

    Exception e = null;
    try {
      obj.put(844, "aosdo");
      fail();
    } catch (LoopExecutionException ex) {
      e = ex;
    }

    assertNotNull(e);
  }

  @Test @SuppressWarnings("unchecked")
  public final void immutableCopyOfMutableObject() {
    LoopObject copy = object.immutize();

    assertNotSame(object, copy);
    assertEquals(object, copy);

    // Mutate original.
    ((List<Integer>)object.get("ls")).set(0, 4);

    assertEquals(Arrays.asList(4, 2, 3), object.get("ls"));
    Object ls = copy.get("ls");
    assertEquals(Arrays.asList(1, 2, 3), ls);

    Exception e = null;
    try {
      ((List)ls).add(4);
      fail();
    } catch (LoopExecutionException ex) {
      e = ex;
    }
    assertNotNull(e);
  }

  @Test @SuppressWarnings("unchecked")
  public final void immutableCopyOfMutableObjectWithCycle() {
    object.put("self", object);

    LoopObject copy = object.immutize();

    assertNotSame(object, copy);

    for (Object key : copy.keySet()) {
      if ("self".equals(key))
        continue;   // equals() seems unable to handle cycles in Java, oh well.

      assertEquals(object.get(key), copy.get(key));
    }

    // Mutate original.
    ((List<Integer>)object.get("ls")).set(0, 4);

    assertEquals(Arrays.asList(4, 2, 3), object.get("ls"));
    Object ls = copy.get("ls");
    assertEquals(Arrays.asList(1, 2, 3), ls);

    Exception e = null;
    try {
      ((List)ls).add(4);
      fail();
    } catch (LoopExecutionException ex) {
      e = ex;
    }
    assertNotNull(e);
  }
}
