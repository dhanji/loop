package loop.ast.script;

import loop.Executable;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads loop source files as individual modules. Module resolution is as follows:
 * <p/>
 * <ol> <li>A single .loop file maps to a leaf module</li> <li>Modules are hierarchical and
 * top-level modules are just directories, so loop.lang must be in loop/lang.loop </li>
 * <li>Requiring 'loop' will import all the concrete sub modules in loop/ (so it won't import any
 * dirs)</li> </ol>
 * <p/>
 * Module resolution order is as follows: <ol> <li>current directory</li> <li>explicit search
 * path</li> </ol>
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ModuleLoader {
  private static final String[] LOOP_FILES = new String[]{".loop"};

  public static volatile String[] searchPaths = new String[] { "." };

  public static void reset() {
    searchPaths = new String[] { "." };
  }

  public static List<Executable> loadAndCompile(List<String> moduleChain) {
    StringBuilder nameBuilder = new StringBuilder();
    for (int i = 0, moduleChainSize = moduleChain.size(); i < moduleChainSize; i++) {
      String part = moduleChain.get(i);
      nameBuilder.append(part);

      if (i < moduleChainSize - 1)
        nameBuilder.append('/');
    }

    String moduleName = nameBuilder.toString();

    // Try the search path in order until we find this module (or not).
    List<File> toLoad = null;
    for (String searchPath : searchPaths) {
      toLoad = search(searchPath + "/" + moduleName);

      if (toLoad != null)
        break;
    }

    if (toLoad == null)
      return null;


    List<Executable> executables = new ArrayList<Executable>();
    for (File toLoadFile : toLoad) {
      try {
        Executable executable = new Executable(new FileReader(toLoadFile), toLoadFile);
        executable.compile();

        executables.add(executable);
      } catch (FileNotFoundException e) {
        return null;
      }
    }

    return executables;
  }

  @SuppressWarnings("unchecked")
  private static List<File> search(String name) {
    List<File> toLoad = new ArrayList<File>();

    // Look in current dir.
    File file = new File(name + ".loop");
    if (!file.exists()) {
      file = new File(name);

      if (!file.exists())
        return null;

      if (file.isDirectory()) {
        // List all loop files and load them.
        toLoad.addAll(FileUtils.listFiles(file, LOOP_FILES, false));

        if (toLoad.isEmpty())
          return null;

      } else
        return null;
    } else
      toLoad.add(file);

    return toLoad;
  }
}
