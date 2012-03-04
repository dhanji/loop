package loop;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class TestObjectWithUnaryCtor {
  private final int i;
  public TestObjectWithUnaryCtor(int i) {
    this.i = i;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TestObjectWithUnaryCtor that = (TestObjectWithUnaryCtor) o;

    if (i != that.i) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return i;
  }
}
