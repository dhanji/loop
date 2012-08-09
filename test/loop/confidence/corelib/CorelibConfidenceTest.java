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
    Loop.run("test/loop/confidence/corelib/file_1.loop");

    assertTrue(new File("target/tmptmp.tmp").exists());
  }
}
