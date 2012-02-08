package loop;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface AnnotatedError {
  String getMessage();

  int line();

  int column();
}
