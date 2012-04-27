package loop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * IO Utils. Copied most of this code from commons-io.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Util {
  private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

  public static String toString(InputStream input) throws IOException {
    StringWriter out = new StringWriter();
    Reader in = new InputStreamReader(input);
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    int n;
    while (-1 != (n = in.read(buffer))) {
      out.write(buffer, 0, n);
    }
    return out.toString();
  }

  public static List<String> toLines(Reader input) throws IOException {
    BufferedReader reader = new BufferedReader(input);
    List<String> list = new ArrayList<String>();
    String line = reader.readLine();
    while (line != null) {
      list.add(line);
      line = reader.readLine();
    }
    return list;
  }

  /**
   * Non-recursive list of files (only) in dir.
   */
  public static Collection<File> listFiles(File directory, final String[] extensions) {
    return Arrays.asList(directory.listFiles(new FilenameFilter() {
      @Override public boolean accept(File file, String s) {
        if (file.isDirectory())
          return false;

        for (String extension : extensions) {
          if (file.getName().endsWith(extension))
            return true;
        }
        return false;
      }
    }));
  }
}
