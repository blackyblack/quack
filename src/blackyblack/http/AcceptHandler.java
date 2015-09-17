package blackyblack.http;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import blackyblack.Application;
import blackyblack.quack.AssetInfo;
import blackyblack.quack.QuackApp;
import nrs.util.Convert;
import nrs.util.Logger;

public final class AcceptHandler extends APITestServlet.APIRequestHandler {
  public static final AcceptHandler instance = new AcceptHandler();

  private AcceptHandler() {
    super("secret", "assets", "recipient", "triggerhash", "finishheight");
  }

  @SuppressWarnings("unchecked")
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws Exception {

    String secret = Convert.emptyToNull(req.getParameter("secret"));
    if (secret == null)
    {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    
    String recipient = Convert.emptyToNull(req.getParameter("recipient"));
    if (recipient != null)
    {
      try
      {
        Convert.parseAccountId(recipient);
      }
      catch(Exception e)
      {
        return JSONResponses.INCORRECT_RECIPIENT;
      }
    }
    
    String assetsString = Convert.emptyToNull(req.getParameter("assets"));
    if (assetsString == null)
    {
      return JSONResponses.MISSING_ASSET;
    }
    
    List<AssetInfo> assets = new ArrayList<AssetInfo>();
    try
    {
      JSONParser parser = new JSONParser();
      JSONArray assetsJson = (JSONArray)parser.parse(assetsString);
      if(assetsJson == null)
      {
        return JSONResponses.INCORRECT_ASSET;
      }
      
      for(Object o : assetsJson)
      {
        JSONObject item = (JSONObject) o;
        AssetInfo a = new AssetInfo();
        a.fromJson(item);
        assets.add(a);
      }
    }
    catch(ParseException e)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    catch(ClassCastException e)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    catch (Exception e)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    
    if(assets.size() == 0)
    {
      return JSONResponses.MISSING_ASSET;
    }
    
    String finishheightValue = Convert.emptyToNull(req.getParameter("finishheight"));
    Long finishheight = 0L;
    if (finishheightValue != null)
    {
      try 
      {
        finishheight = Long.parseLong(finishheightValue);
      }
      catch (RuntimeException e)
      {
      }
    }

    //default height is currentHeight + 620 blocks
    ///HACK: default height will likely be invalid. Make it 100 blocks less than init default height
    if(finishheight == 0)
    {
      Long height = Application.api.getCurrentBlock();
      finishheight = height + 620L;
    }
    
    String triggerhash = Convert.emptyToNull(req.getParameter("triggerhash"));
    if (triggerhash == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }

    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      answer = (JSONObject)QuackApp.instance.accept(secret, recipient, finishheight.intValue(), assets, triggerhash);
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
