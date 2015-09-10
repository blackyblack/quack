package blackyblack.http;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nrs.Constants;
import nrs.NxtException.NxtApiException;
import nrs.crypto.Crypto;
import nrs.util.Convert;
import nrs.util.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import blackyblack.AppConstants;
import blackyblack.Application;
import blackyblack.AssetInfo;
import blackyblack.NxtApi;

/*
 * trigger = trigger tx json
 * fullhash = trigger tx full hash
 * swapid = trigger tx unsigned bytes
 * A = initiator
 * B = acceptor
 * 
 * 1. A: quackInit and send invitation to B
 * 2. B: quackCheck(swapid) and quackAccept
 * 3. A: quackCheck(swapid) and quackValidate
 * 4. A: quackTrigger(swapid)
 * 
 * How to restore swapid:
 * a. Check invitation
 * b. Make offline copy
 * c. Scan for init transactions (duplicated in each tx)
 */

public final class InitiateHandler extends APITestServlet.APIRequestHandler {
  public static final InitiateHandler instance = new InitiateHandler();

  private InitiateHandler() {
    super("secret", "assets", "recipient", "timeout");
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
    
    

    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      answer = (JSONObject)quackInit(secret, recipient, timeout, assets);
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
  public JSONStreamAware quackInit(String secret, String recipient, long timeout, List<AssetInfo> assets) throws NxtApiException
  {
    //now prepare triggertx and send phased transfers
    Long height = Application.api.getCurrentBlock();
    
    JSONObject trigger = createtrigger(AppConstants.triggerAccount, secret, 1440, AppConstants.triggerFee);
    String fullhash = Application.api.getFullHash(trigger);
    if(fullhash == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }
    
    String unsignedtrigger = Application.api.getUnsignedBytes(trigger);
    if(unsignedtrigger == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }
    
    int deadline = (int)timeout / 2;
    if(deadline < 3) deadline = 3;
    if(deadline + 1 > timeout) timeout = deadline + 1;
    
    for(AssetInfo a : assets)
    {
      if(a.id == null) continue;
      if(a.id.equals("5527630"))
      {
        JSONObject paytx = Application.api.createPhasedPayment(recipient, secret, fullhash, unsignedtrigger, deadline, height + timeout,
            a.quantity, "");
        if(paytx != null)
        {
          String txid = (String) paytx.get("transaction");
          Logger.logMessage("Queued transaction: " + txid + "; finish at " + height + timeout);
        }
        continue;
      }
      
      JSONObject paytx = Application.api.createPhasedAsset(recipient, secret, fullhash, unsignedtrigger, deadline, height + timeout,
          a.id, a.quantity, "");
      if(paytx != null)
      {
        String txid = (String) paytx.get("transaction");
        Logger.logMessage("Queued transaction: " + txid + "; finish at " + height + timeout);
      }
      continue;
    }
    
    JSONObject answer = new JSONObject();
    answer.put("query_status", "good");
    answer.put("swapid", unsignedtrigger);
    answer.put("triggerhash", fullhash);
    return answer;
  }
  
  @SuppressWarnings("unchecked")
  public JSONObject createtrigger(String recipient, String secretPhrase,
      int deadline, long payment) throws NxtApiException
  {
    JSONObject messageJson = new JSONObject();
    messageJson.put("quack", 1L);
    messageJson.put("trigger", 1L);
    messageJson.put("nonce", "" + Crypto.nextRandom());
    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "sendMoney"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("secretPhrase", secretPhrase));
    fields.add(new BasicNameValuePair("feeNQT", "" + Constants.ONE_NXT));
    fields.add(new BasicNameValuePair("broadcast", "false"));
    fields.add(new BasicNameValuePair("deadline", "" + deadline));
    fields.add(new BasicNameValuePair("amountNQT", "" + payment));
    fields.add(new BasicNameValuePair("message", messageJson.toString()));
    fields.add(new BasicNameValuePair("messageIsText", "true"));
    
    CloseableHttpResponse response = null;
    JSONObject json = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(NxtApi.api());
        http.setHeader("Origin", NxtApi.host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        json = (JSONObject)parser.parse(content);
        if(json == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return json;
  }
}
