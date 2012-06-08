package loop.confidence.interop;

/**
 * Testing interface for Java -> Loop communication.
 *
 * @author galdolber
 */
public interface ExampleJavaInterface {
  
  double multiply(double a, double b);
  
  String sayHello(String name);
  
  String sayHello(Person person);
  
  String noArgumentsMethod();
  
  void unexistingMethod();
}
