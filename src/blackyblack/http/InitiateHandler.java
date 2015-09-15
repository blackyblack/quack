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

import blackyblack.quack.AssetInfo;
import blackyblack.quack.QuackApp;
import nrs.util.Convert;
import nrs.util.Logger;

public final class InitiateHandler extends APITestServlet.APIRequestHandler {
  public static final InitiateHandler instance = new InitiateHandler();

  private InitiateHandler() {
    super("secret", "assets", "expected_assets", "recipient", "timeout");
  }

  @SuppressWarnings("unchecked")
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws Exception {

    JSONParser parser = new JSONParser();
	  
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
    
    String expectedString = Convert.emptyToNull(req.getParameter("expected_assets"));
    if (expectedString == null)
    {
      return JSONResponses.MISSING_ASSET;
    }
    
    List<AssetInfo> expectedAssets = new ArrayList<AssetInfo>();
    try
    {
      JSONArray assetsJson = (JSONArray)parser.parse(expectedString);
      if(assetsJson == null)
      {
        return JSONResponses.INCORRECT_ASSET;
      }
      
      for(Object o : assetsJson)
      {
        JSONObject item = (JSONObject) o;
        AssetInfo a = new AssetInfo();
        a.fromJson(item);
        expectedAssets.add(a);
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
    
    String timeoutValue = Convert.emptyToNull(req.getParameter("timeout"));
    Long timeout = 0L;
    if (timeoutValue != null)
    {
      try 
      {
        timeout = Long.parseLong(timeoutValue);
      }
      catch (RuntimeException e)
      {
      }
    }

    //default timeout is 720 blocks
    if(timeout == 0)
    {
      timeout = 720L;
    }
    
    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      answer = (JSONObject)QuackApp.instance.init(secret, recipient, timeout, assets, expectedAssets);
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
