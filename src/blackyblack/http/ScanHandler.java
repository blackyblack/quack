package blackyblack.http;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import blackyblack.quack.AssetInfo;
import blackyblack.quack.BlockAssetInfo;
import blackyblack.quack.QuackApp;
import blackyblack.quack.SwapInfo;
import nrs.util.Convert;
import nrs.util.Logger;

public final class ScanHandler extends APITestServlet.APIRequestHandler {
  public static final ScanHandler instance = new ScanHandler();

  private ScanHandler() {
    super("account", "timelimit");
  }

  @SuppressWarnings("unchecked")
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws Exception {

    String account = Convert.emptyToNull(req.getParameter("account"));
    if (account == null)
    {
      return JSONResponses.MISSING_ACCOUNT;
    }
    
    String timelimitValue = Convert.emptyToNull(req.getParameter("timelimit"));
    Long timelimit = 0L;
    if (timelimitValue != null)
    {
      try 
      {
        timelimit = Long.parseLong(timelimitValue);
      }
      catch (RuntimeException e)
      {
      }
    }

    //default timelimit is 7 days
    if(timelimit == 0)
    {
      timelimit = (60L * 60 * 24 * 7);
    }

    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      List<SwapInfo> swaps = QuackApp.instance.scanSwaps(account, timelimit.intValue());
      JSONArray swapArray = new JSONArray();
      for(SwapInfo a : swaps)
      {
        JSONObject swapResult = new JSONObject();
        swapResult.put("triggerBytes", a.triggerBytes);
        swapResult.put("triggerPrunnableBytes", a.triggerPrunnableBytes);
        swapResult.put("triggerhash", a.triggerhash);
        swapResult.put("sender", a.sender);
        swapResult.put("recipient", a.recipient);
        swapResult.put("minFinishHeight", a.minFinishHeight);
        swapResult.put("gotTrigger", a.gotTrigger);
        
        JSONArray assetsArray = new JSONArray();
        for (AssetInfo b : a.announcedAssets)
        {
          assetsArray.add(b.toJson());
        }
        swapResult.put("announcedAssets", assetsArray);

        assetsArray = new JSONArray();
        for (AssetInfo b : a.announcedExpAssets)
        {
          assetsArray.add(b.toJson());
        }
        swapResult.put("expectedAssets", assetsArray);
        
        assetsArray = new JSONArray();
        for (BlockAssetInfo b : a.assetsA)
        {
          assetsArray.add(b.toJson());
        }
        swapResult.put("assetsA", assetsArray);
        
        assetsArray = new JSONArray();
        for (BlockAssetInfo b : a.assetsB)
        {
          assetsArray.add(b.toJson());
        }
        swapResult.put("assetsB", assetsArray);
        
        swapArray.add(swapResult);
      }
      
      answer.put("query_status", "good");
      answer.put("swaps", swapArray);
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
