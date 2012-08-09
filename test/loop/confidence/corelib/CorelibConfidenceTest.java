package loop.confidence.corelib;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class CorelibConfidenceTest extends LoopTest {

  @Test
  public final void simpleFileWrite() throws IOException {
    File expected = new File("target/tmptmp.tmp");
    expected.delete();
    Loop.run("test/loop/confidence/corelib/file_1.loop");

    assertTrue(expected.exists());
    expected.deleteOnExit();
  }
}
