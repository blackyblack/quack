package blackyblack.http;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nrs.util.Convert;
import nrs.util.JSON;
import nrs.util.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import blackyblack.Application;


public class APITestServlet extends HttpServlet {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  static final Map<String,APIRequestHandler> apiRequestHandlers;
  
  abstract static class APIRequestHandler {

    private final List<String> parameters;

    APIRequestHandler(String... parameters) {
        this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
    }

    final List<String> getParameters() {
        return parameters;
    }

    abstract JSONStreamAware processRequest(HttpServletRequest request) throws Exception;

    boolean requirePost() {
        return false;
    }
}
  
  static {
    Map<String,APIRequestHandler> map = new HashMap<>();
    
    map.put("init", InitiateHandler.instance);
    map.put("accept", AcceptHandler.instance);
    map.put("scan", ScanHandler.instance);
    map.put("trigger", TriggerHandler.instance);
    
    apiRequestHandlers = Collections.unmodifiableMap(map);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      process(req, resp);
  }
  
  private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException 
  {
    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    resp.setHeader("Pragma", "no-cache");
    resp.setDateHeader("Expires", 0);

    JSONStreamAware response = JSON.prepare(new JSONObject());

    try
    {      
      if (Application.allowedBotHosts != null && ! Application.allowedBotHosts.contains(req.getRemoteHost()))
      {
        response = JSONResponses.ERROR_NOT_ALLOWED;
        return;
      }

      if (! "POST".equals(req.getMethod()))
      {
        response = JSONResponses.POST_REQUIRED;
        return;
      }
      
      String command = Convert.emptyToNull(req.getParameter("requestType"));
      APIRequestHandler apiRequestHandler = apiRequestHandlers.get(command);
      response = apiRequestHandler.processRequest(req);
    }
    catch (Exception e) 
    {
      Logger.logMessage("Error processing API request", e);
      response = JSONResponses.ERROR_INCORRECT_REQUEST;
    }
    finally
    {
        resp.setContentType("text/plain; charset=UTF-8");
        try (Writer writer = resp.getWriter())
        {
          response.writeJSONString(writer);
        }
    }
  }
}
