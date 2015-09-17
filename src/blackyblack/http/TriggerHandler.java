package blackyblack.http;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import blackyblack.quack.QuackApp;
import nrs.util.Convert;
import nrs.util.Logger;

public final class TriggerHandler extends APITestServlet.APIRequestHandler {
  public static final TriggerHandler instance = new TriggerHandler();

  private TriggerHandler() {
    super("secret", "triggerBytes");
  }

  @SuppressWarnings("unchecked")
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws Exception {

    String secret = Convert.emptyToNull(req.getParameter("secret"));
    if (secret == null)
    {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    
    String triggerBytes = Convert.emptyToNull(req.getParameter("triggerBytes"));
    if (triggerBytes == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }

    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      answer = (JSONObject)QuackApp.instance.trigger(secret, triggerBytes);
    }
    catch (Exception e)
    {
      Logger.logMessage("Error in NRS API call: " + e.getMessage());

      answer.put("errorCode", 9);
      answer.put("errorDescription", e.getMessage());
      answer.put("error", e.getMessage());
    }
    finally
    {
      if (response != null)
        response.close();
    }
    return answer;
  }

  @Override
  boolean requirePost() {
    return true;
  }
}
