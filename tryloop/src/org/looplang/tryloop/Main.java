package org.looplang.tryloop;

import loop.Loop;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class Main {
  public static void main(String[] args) throws Exception {
    Server server = new Server(8080);
    server.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target,
                         Request baseRequest,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException, ServletException {

        HashMap<String, Object> context = new HashMap<String, Object>();
        StringBuilder out = new StringBuilder();
        context.put("__out", out);

        String script = request.getParameter("script");

        // Prepend our special print function, to intercept the normal print behavior.
        script = "print(obj) ->\n" +
            "       __out.append(obj)\n\n" + script;
        Object result = Loop.run(script, false, context);

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        String stdout = out.toString();
        if (!stdout.trim().isEmpty()) {
          writer.print(stdout);
          writer.print('\n');
        }
        if (null != result)
          writer.print(result);
      }
    });

    server.start();
    server.join();
  }
}
