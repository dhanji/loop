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
 *
 * <ol>
 *   <li>A single .loop file maps to a leaf module</li>
 *   <li>Modules are hierarchical and top-level modules are just directories, so loop.lang must
 *        be in loop/lang.loop </li>
 *   <li>Requiring 'loop' will import all the concrete sub modules in loop/</li>
 * </ol>
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ModuleLoader {
  private static final String[] LOOP_FILES = new String[]{ ".loop" };

  @SuppressWarnings("unchecked")
  public static List<Executable> loadAndCompile(List<String> moduleChain) {
    StringBuilder nameBuilder = new StringBuilder();
    for (int i = 0, moduleChainSize = moduleChain.size(); i < moduleChainSize; i++) {
      String part = moduleChain.get(i);
      nameBuilder.append(part);

      if (i < moduleChainSize - 1)
        nameBuilder.append('/');
    }

    List<File> toLoad = new ArrayList<File>();

    File file = new File(nameBuilder.toString() + ".loop");
    if (!file.exists()) {
      file = new File(nameBuilder.toString());

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

    List<Executable> executables = new ArrayList<Executable>();
    for (File toLoadFile : toLoad) {
      try {
        Executable executable = new Executable(new FileReader(toLoadFile));
        executable.compile();

        executables.add(executable);
      } catch (FileNotFoundException e) {
        return null;
      }
    }

    return executables;
  }
}
