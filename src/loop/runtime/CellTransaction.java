package loop.runtime;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
* @author dhanji@gmail.com (Dhanji R. Prasanna)
*/
public class CellTransaction implements Map {
  private final String name;
  private final Cells.VersionedCell cell;

  public CellTransaction(String name, Cells.VersionedCell cell) {
    this.name = name;
    this.cell = cell; }

  public void commit() {
    // Replace with the new version.
    Cells.updateCell(name, cell);
  }

  @Override public int size() {
    return cell.object.size();
  }

  @Override public boolean isEmpty() {
    return cell.object.isEmpty();
  }

  @Override public boolean containsKey(Object o) {
    return cell.object.containsKey(o);
  }

  @Override public boolean containsValue(Object o) {
    return cell.object.containsValue(o);
  }

  @Override public Object get(Object o) {
    return cell.object.get(o);
  }

  @Override public Object put(Object o, Object o1) {
    return cell.object.put(o, o1);
  }

  @Override public Object remove(Object o) {
    return cell.object.remove(o);
  }

  @Override public void putAll(Map map) {
    throw new UnsupportedOperationException();
  }

  @Override public void clear() {
    cell.object.clear();
  }

  @Override public Set keySet() {
    return cell.object.keySet();
  }

  @Override public Collection values() {
    return cell.object.values();
  }

  @Override public Set entrySet() {
    return cell.object.entrySet();
  }
}
