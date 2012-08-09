package loop.ast.script;

import loop.Executable;
import loop.LoopClassLoader;
import loop.Util;
import loop.lang.LoopClass;
import loop.runtime.Caller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

  public static volatile String[] searchPaths = new String[]{ "." };

  private static final Set<String> CORE_MODULES = new HashSet<String>(Arrays.asList(
      "prelude",
      "console",
      "channels",
      "file"
  ));

  // For faster loading of core modules.
  private static final ConcurrentMap<String, String> corelibCache =
      new ConcurrentHashMap<String, String>();

  // Prevents cyclical reloading of identical modules.
  private static final ConcurrentMap<String, List<Executable>> loadedModules =
      new ConcurrentHashMap<String, List<Executable>>();

  public static void reset() {
    searchPaths = new String[]{"."};

    loadedModules.clear();
    Caller.reset();
    LoopClassLoader.reset();
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
    List<Executable> executables = loadedModules.get(moduleName);

    if (null != executables)
      return executables;

    List<Reader> toLoad = null;

    // First try to load this module from our resource package (i.e. boot loader)
    if (CORE_MODULES.contains(moduleName)) {
      String cached = corelibCache.get(moduleName);
      if (cached == null) {
        InputStream resourceStream = LoopClass.class.getResourceAsStream(moduleName + ".loop");
        if (null == resourceStream)
          return null;

        try {
          cached = Util.toString(resourceStream).intern();
        } catch (IOException e) {
          return null;
        }

        corelibCache.putIfAbsent(moduleName, cached);
      }

      toLoad = Arrays.<Reader>asList(new StringReader(cached));
    } else {
      // Try the search path in order until we find this module (or not).
      // A module may be composed of many concrete submodules. So we return
      // many executables.
      for (String searchPath : searchPaths) {
        toLoad = search(searchPath + "/" + moduleName);

        if (toLoad != null)
          break;
      }
    }

    if (toLoad == null || toLoad.isEmpty())
      return null;


    executables = new ArrayList<Executable>();
    for (Reader toLoadFile : toLoad) {
      Executable executable = new Executable(toLoadFile, moduleName);
      try {
        toLoadFile.close();
      } catch (IOException e) {
        // Ignore.
      }

      executable.compile();
      executables.add(executable);
    }

    List<Executable> other = loadedModules.putIfAbsent(moduleName, executables);

    return other != null ? other : executables;
  }

  @SuppressWarnings("unchecked")
  private static List<Reader> search(String name) {
    List<Reader> toLoad = new ArrayList<Reader>();

    // Look in current dir.
    File file = new File(name + ".loop");
    if (!file.exists()) {
      file = new File(name);

      if (!file.exists())
        return null;

      if (file.isDirectory()) {
        // List all loop files and load them.
        Collection<File> files = Util.listFiles(file, LOOP_FILES);
        for (File dep : files) {
          try {
            toLoad.add(new FileReader(dep));
          } catch (FileNotFoundException e) {
            // Weird, should not happen!
            e.printStackTrace();
          }
        }

        if (toLoad.isEmpty())
          return null;

      } else
        return null;
    } else
      try {
        toLoad.add(new FileReader(file));
      } catch (FileNotFoundException e) {
        // This should never happen.
      }

    return toLoad;
  }
}
