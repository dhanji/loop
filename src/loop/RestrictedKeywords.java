package loop;

import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class RestrictedKeywords {
  public static final String EXCEPT = "except";
  public static final String AS = "as";
  public static final String ENSURE = "@ensure";

  public static boolean isStaticOperator(List<Token> tokens) {
    if (null == tokens || tokens.size() != 2)
      return false;
    for (Token token : tokens) {
      if (!":".equals(token.value))
        return false;
    }
    return true;
  }
}
