package loop.confidence.concurrency;

import loop.Loop;
import loop.LoopTest;
import org.junit.Test;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ChannelsConfidenceTest extends LoopTest {

  @Test
  public final void printerBurst() {
    // Counts upto 10 on global worker pool.
    Loop.run("test/loop/confidence/concurrency/channels_printer.loop");
  }

  @Test
  public final void printerSerial() {
    // Counts upto 10 on global worker pool.
    Loop.run("test/loop/confidence/concurrency/channels_printer_2.loop");
  }

//  @Test TODO TEMPORARILY DISABLED
  public final void counterSerial() {
    Loop.run("test/loop/confidence/concurrency/channels_counter.loop");
  }

  @Test
  public final void pingpongBurst() throws InterruptedException {
    // Counts upto 10 on global worker pool.
    Loop.run("test/loop/confidence/concurrency/channels_pingpong.loop");
    Thread.sleep(5);
  }
}
