package loop;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class LoopShell {
  public static void shell() {
    try {
      ConsoleReader reader = new ConsoleReader();
      reader.addCompleter(new MetaCommandCompleter());

      Map<String, Object> context = new HashMap<String, Object>();
      boolean active = true;
      do {

        String rawLine = reader.readLine("> ");
        if (rawLine == null) {
          quit();
        }

        String line = rawLine.trim();
        System.out.println(reader.getCompleters());

        if (line.startsWith(":q") || line.startsWith(":quit")) {
          quit();
        }

        if (isLoadCommand(line)) {
          System.out.println("Loaded.");
          System.exit(0);
        }



      } while (active);
      System.exit(0);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isLoadCommand(String line) {
    return line.startsWith(":l ") || line.startsWith(":load ");
  }

  private static void quit() {
    System.out.println("Bye.");
    System.exit(0);
  }

  private static class MetaCommandCompleter implements Completer {
    private final List<String> commands = Arrays.asList(
        ":load",
        ":quit"
    );

    private final FileNameCompleter fileNameCompleter = new FileNameCompleter();

    @Override public int complete(String buffer, int cursor, List<CharSequence> candidates) {
      if (buffer == null) {
        buffer = "";
      } else
        buffer = buffer.trim();

      // See if we should chain to the filename completer first.
      if (isLoadCommand(buffer)) {
        String[] split = buffer.split("[ ]+");
        if (split.length > 2)
          return cursor;

        // Always complete the first argument.
        return fileNameCompleter.complete(split[1], cursor, candidates);
      }

      for (String command : commands) {
        if (command.startsWith(buffer)) {
          candidates.add(command.substring(buffer.length()) + ' ');
        }
      }

      return cursor;
    }
  }
}
