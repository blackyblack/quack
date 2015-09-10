package blackyblack.http;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nrs.NxtException.NxtApiException;
import nrs.util.Convert;
import nrs.util.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import blackyblack.Application;
import blackyblack.AssetInfo;

public final class AcceptHandler extends APITestServlet.APIRequestHandler {
  public static final AcceptHandler instance = new AcceptHandler();

  private AcceptHandler() {
    super("secret", "assets", "recipient", "swapid", "triggerhash", "timeout");
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
        String assetid = Convert.emptyToNull((String) item.get("assetid"));
        Long quantity = Convert.nullToZero((Long) item.get("quantity"));
        if(assetid == null) continue;
        if(quantity == 0) continue;
        
        AssetInfo a = new AssetInfo();
        a.name = "";
        a.id = assetid;
        a.quantity = quantity;
        a.decimals = 0;
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
    
    String swapid = Convert.emptyToNull(req.getParameter("swapid"));
    if (swapid == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
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
      answer = (JSONObject)quackAccept(secret, recipient, timeout.intValue(), assets, swapid, triggerhash);
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
  
  @SuppressWarnings("unchecked")
  public JSONStreamAware quackAccept(String secret, String recipient, int finishHeight, List<AssetInfo> assets,
      String swapid, String triggerhash) throws NxtApiException
  {
    //now prepare triggertx and send phased transfers
    Long height = Application.api.getCurrentBlock();
    int rest = finishHeight - height.intValue();
    
    if(rest <= 0)
    {
      throw new NxtApiException("Too short period until timeout");
    }
    
    int deadline = rest / 2;
    if(deadline < 3) deadline = 3;
    if((deadline + 1) > rest)
    {
      throw new NxtApiException("Too short period until timeout");
    }
    
    for(AssetInfo a : assets)
    {
      if(a.id == null) continue;
      if(a.id.equals("5527630"))
      {
        JSONObject paytx = Application.api.createPhasedPayment(recipient, secret, triggerhash, swapid, deadline, finishHeight,
            a.quantity, "");
        if(paytx != null)
        {
          String txid = (String) paytx.get("transaction");
          Logger.logMessage("Queued transaction: " + txid + "; finish at " + finishHeight);
        }
        continue;
      }
      
      JSONObject paytx = Application.api.createPhasedAsset(recipient, secret, triggerhash, swapid, deadline, finishHeight,
          a.id, a.quantity, "");
      if(paytx != null)
      {
        String txid = (String) paytx.get("transaction");
        Logger.logMessage("Queued transaction: " + txid + "; finish at " + finishHeight);
      }
      continue;
    }
    
    JSONObject answer = new JSONObject();
    answer.put("query_status", "good");
    return answer;
  }
}
