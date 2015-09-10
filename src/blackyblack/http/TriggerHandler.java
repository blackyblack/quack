package blackyblack.http;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nrs.NxtException.NxtApiException;
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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.parser.JSONParser;

import blackyblack.NxtApi;

public final class TriggerHandler extends APITestServlet.APIRequestHandler {
  public static final TriggerHandler instance = new TriggerHandler();

  private TriggerHandler() {
    super("secret", "swapid");
  }

  @SuppressWarnings("unchecked")
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws Exception {

    String secret = Convert.emptyToNull(req.getParameter("secret"));
    if (secret == null)
    {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    
    String swapid = Convert.emptyToNull(req.getParameter("swapid"));
    if (swapid != null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }

    JSONObject answer = new JSONObject();
    CloseableHttpResponse response = null;
    try
    {
      answer = (JSONObject)quackTrigger(secret, swapid);
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
  public JSONStreamAware quackTrigger(String secret, String swapid) throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "signTransaction"));
    fields.add(new BasicNameValuePair("unsignedTransactionBytes", swapid));
    fields.add(new BasicNameValuePair("secretPhrase", secret));
    
    CloseableHttpResponse response = null;
    JSONObject json = null;
    String txBytes = null;
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

        txBytes = (String)json.get("transactionBytes");
        if(txBytes == null)
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
    
    fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "broadcastTransaction"));
    fields.add(new BasicNameValuePair("transactionBytes", txBytes));
    
    response = null;
    String txid = null;
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

        txid = (String)json.get("transaction");
        if(txid == null)
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
    
    JSONObject answer = new JSONObject();
    answer.put("query_status", "good");
    answer.put("txid", txid);
    return answer;
  }
}
