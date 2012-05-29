package loop.runtime;

import loop.lang.ImmutableLoopObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Software transactional memory support for loop's "cell memory" concurrency construct.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Cells {
  private static final ConcurrentMap<String, VersionedCell> cells =
      new ConcurrentHashMap<String, VersionedCell>();

  public static boolean updateCell(String cell, VersionedCell newCell) {
    VersionedCell versionedCell = cells.putIfAbsent(cell, newCell);

    // If this cell was already present, update it in place.
    return versionedCell == null
        || cells.replace(cell, new VersionedCell(newCell.version - 1, null), newCell);
  }

  public static Object readCell(String cell) {
    VersionedCell versionedCell = cells.get(cell);

    return versionedCell != null ? versionedCell.object : null;
  }

  public static Object beginTransaction(String cell) {
    return new CellTransaction(cell, cells.get(cell));
  }

  public static boolean evictCell(String cell, Integer version) {
    return cells.remove(cell, new VersionedCell(version, null));
  }

  public static class VersionedCell {
    public final int version;
    public final ImmutableLoopObject object;

    public VersionedCell(int version, ImmutableLoopObject object) {
      this.version = version;
      this.object = object;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      VersionedCell that = (VersionedCell) o;

      return version == that.version;
    }

    @Override
    public int hashCode() {
      return version;
    }
  }
}
