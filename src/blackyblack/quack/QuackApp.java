package blackyblack.quack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import blackyblack.AppConstants;
import blackyblack.Application;
import blackyblack.INxtApi;
import blackyblack.NxtApi;
import blackyblack.http.JSONResponses;
import nrs.Constants;
import nrs.NxtException.NxtApiException;
import nrs.crypto.Crypto;
import nrs.util.Convert;
import nrs.util.Logger;

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
public class QuackApp
{
  public static final QuackApp instance = new QuackApp();
  public INxtApi api = new NxtApi();

  public String marketAccount;

  private QuackApp()
  {
  }

  @SuppressWarnings("unchecked")
  public JSONStreamAware init(String secret, String recipient, long timeout, List<AssetInfo> assets, List<AssetInfo> expectedAssets) throws NxtApiException
  {
    // now prepare triggertx and send phased transfers
    Long height = Application.api.getCurrentBlock();

    JSONObject trigger = createtrigger(AppConstants.triggerAccount, secret, recipient, 1440, AppConstants.triggerFee, assets, expectedAssets);
    String fullhash = Application.api.getFullHash(trigger);
    if (fullhash == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }

    String unsignedtrigger = Application.api.getUnsignedBytes(trigger);
    if (unsignedtrigger == null)
    {
      return JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;
    }

    int deadline = (int) timeout / 2;
    if (deadline < 3)
      deadline = 3;
    if (deadline + 1 > timeout)
      timeout = deadline + 1;

    for (AssetInfo a : assets)
    {
      if (a.id == null)
        continue;
      if (a.type.equals("NXT"))
      {
        JSONObject paytx = phasedPayment(recipient, secret, fullhash, unsignedtrigger, deadline, height + timeout, a.quantity, "");
        if (paytx != null)
        {
          String txid = (String) paytx.get("transaction");
          Logger.logMessage("Queued transaction: " + txid + "; finish at " + (height + timeout));
        }
        continue;
      }

      JSONObject paytx = phasedAsset(recipient, secret, fullhash, unsignedtrigger, deadline, height + timeout, a.id, a.quantity, "");
      if (paytx != null)
      {
        String txid = (String) paytx.get("transaction");
        Logger.logMessage("Queued transaction: " + txid + "; finish at " + (height + timeout));
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
  public JSONObject createtrigger(String recipient, String secretPhrase, String acceptor, int deadline, long payment,
      List<AssetInfo> assets, List<AssetInfo> expectedAssets) throws NxtApiException
  {
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    Long accountId = Convert.publicKeyToAccountId(publicKey);
    String sender = Convert.rsAccount(accountId);
    
    JSONObject messageJson = new JSONObject();
    messageJson.put("quack", 1L);
    messageJson.put("trigger", 1L);
    messageJson.put("sender", sender);
    messageJson.put("recipient", acceptor);
    messageJson.put("nonce", "" + Crypto.nextRandom());
    JSONArray assetsArray = new JSONArray();
    for (AssetInfo a : assets)
    {
      assetsArray.add(a.toJson());
    }
    messageJson.put("assets", assetsArray);

    assetsArray = new JSONArray();
    for (AssetInfo a : expectedAssets)
    {
      assetsArray.add(a.toJson());
    }
    messageJson.put("expected_assets", assetsArray);

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
        json = (JSONObject) parser.parse(content);
        if (json == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }
      }
      finally
      {
        if (response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  public JSONStreamAware trigger(String secret, String swapid) throws NxtApiException
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
        json = (JSONObject) parser.parse(content);
        if (json == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }

        txBytes = (String) json.get("transactionBytes");
        if (txBytes == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }
      }
      finally
      {
        if (response != null)
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
        json = (JSONObject) parser.parse(content);
        if (json == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }

        txid = (String) json.get("transaction");
        if (txid == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }
      }
      finally
      {
        if (response != null)
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

  @SuppressWarnings("unchecked")
  public JSONStreamAware accept(String secret, String recipient, int finishHeight, List<AssetInfo> assets, String swapid, String triggerhash) throws NxtApiException
  {
    // now prepare triggertx and send phased transfers
    Long height = Application.api.getCurrentBlock();
    int rest = finishHeight - height.intValue();

    if (rest <= 0)
    {
      throw new NxtApiException("Too short period until timeout");
    }

    int deadline = rest / 2;
    if (deadline < 3)
      deadline = 3;
    if ((deadline + 1) > rest)
    {
      throw new NxtApiException("Too short period until timeout");
    }

    for (AssetInfo a : assets)
    {
      if (a.id == null)
        continue;
      if (a.type.equals("NXT"))
      {
        JSONObject paytx = phasedPayment(recipient, secret, triggerhash, swapid, deadline, finishHeight, a.quantity, "");
        if (paytx != null)
        {
          String txid = (String) paytx.get("transaction");
          Logger.logMessage("Queued transaction: " + txid + "; finish at " + finishHeight);
        }
        continue;
      }

      JSONObject paytx = phasedAsset(recipient, secret, triggerhash, swapid, deadline, finishHeight, a.id, a.quantity, "");
      if (paytx != null)
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
  
  public List<SwapInfo> scanSwaps(String account, int timelimit) throws NxtApiException
  {
    List<SwapInfo> result = new ArrayList<SwapInfo>();
    //map with fullhash as a key
    Map<String, SwapInfo> lookup = new HashMap<String, SwapInfo>();
    //get account transactions down to minHeight
    //look for transactions with quack id
    //combine together transactions with same swapid and trigger=swapid
    List<JSONObject> txs = api.getTransactions(account, timelimit);
    JSONParser parser = new JSONParser();
    for(JSONObject tx : txs)
    {
      try
      {
        if(tx == null) continue;
        JSONObject attach = (JSONObject) tx.get("attachment");
        if(attach == null) continue;
        String message = (String) attach.get("message");
        if(message == null) continue;
        JSONObject data = (JSONObject) parser.parse(message);
        if(data == null) continue;
        
        if(!isQuack(data)) continue;
        if(isTrigger(data))
        {
          //find fullhash in a map and add trigger here
          String fullhash = Convert.emptyToNull((String) tx.get("fullHash"));
          if(fullhash == null) continue;
          SwapInfo x = lookup.get(fullhash);
          if(x == null)
          {
            x = new SwapInfo();
          }
          
          x.gotTrigger = true;         
          lookup.put(fullhash, x);
          continue;
        }
        
        //phased transactions are added by checking linked fullhash
        String swapid = getSwapid(data);
        if(swapid == null) continue;
        if(swapid.equals("")) continue;
        
        JSONArray linkedhashes = (JSONArray) attach.get("phasingLinkedFullHashes");
        if(linkedhashes == null) continue;
        if(linkedhashes.size() == 0) continue;
        
        String hashdata = Convert.emptyToNull((String) linkedhashes.get(0));
        if(hashdata == null) continue;
        
        String txSender = Convert.emptyToNull((String) tx.get("senderRS"));
        String txRecipient = Convert.emptyToNull((String) tx.get("recipientRS"));
        
        if(txSender == null) continue;
        if(txRecipient == null) continue;
        
        SwapInfo x = lookup.get(hashdata);
        if(x == null)
        {
          x = new SwapInfo();
        }
        
        //first swapid will create swap info
        if(x.announcedAssets.size() == 0)
        {
          JSONObject swapTx = api.parseTransaction(swapid);
          
          if(swapTx == null) continue;
          attach = (JSONObject) swapTx.get("attachment");
          if(attach == null) continue;
          message = (String) attach.get("message");
          data = (JSONObject) parser.parse(message);
          if(data == null) continue;
          
          //parse swapid to get initiator and acceptor
          x.sender = Convert.emptyToNull((String) data.get("sender"));
          x.recipient = Convert.emptyToNull((String) data.get("recipient"));
          x.swapid = swapid;
          x.triggerhash = hashdata;
          
          if(x.sender == null) continue;
          if(x.recipient == null) continue;
          
          //parse swapid to get assets and expected_assets
          x.announcedAssets = new ArrayList<AssetInfo>();
          JSONArray annAssets = (JSONArray) data.get("assets");
          if(annAssets != null)
          {
            for(Object o : annAssets)
            {
              JSONObject j = (JSONObject) o;
              AssetInfo a = new AssetInfo();
              a.fromJson(j);
              x.announcedAssets.add(a);
            }
          }
          
          x.announcedExpAssets = new ArrayList<AssetInfo>();
          annAssets = (JSONArray) data.get("expected_assets");
          if(annAssets != null)
          {
            for(Object o : annAssets)
            {
              JSONObject j = (JSONObject) o;
              AssetInfo a = new AssetInfo();
              a.fromJson(j);
              x.announcedExpAssets.add(a);
            }
          }
        }
        
        BlockAssetInfo assetInfo = new BlockAssetInfo();
        assetInfo.tx = tx;
        AssetInfo assetInfoData = new AssetInfo();
        
        Long txType = 0L;
        Long txSubtype = 0L;
        
        txType = Convert.nullToZero((Long) tx.get("type"));
        txSubtype= Convert.nullToZero((Long) tx.get("subtype"));
        
        //check if it is payment
        if(txType == 0 && txSubtype == 0)
        {
          assetInfoData.id = "1";
          assetInfoData.type = "NXT";
          String qnt = Convert.emptyToNull((String) tx.get("amountNQT"));
          if(qnt != null)
          {
            assetInfoData.quantity = Long.parseLong(qnt);
          }
          
          assetInfo.asset = assetInfoData;
          
          if(txSender.equals(x.sender))
          {
            x.assetsA.add(assetInfo);
            lookup.put(hashdata, x);
          }
          else if(txSender.equals(x.recipient))
          {
            x.assetsB.add(assetInfo);
            lookup.put(hashdata, x);
          }
          
          continue;
        }
        
        //check if it is asset transfer
        if(txType == 2 && txSubtype == 1)
        {
          assetInfoData.id = Convert.emptyToNull((String) attach.get("asset"));
          String qnt = Convert.emptyToNull((String) attach.get("quantityQNT"));
          if(qnt != null)
          {
            assetInfoData.quantity = Long.parseLong(qnt);
          }
          assetInfoData.type = "A";
          
          assetInfo.asset = assetInfoData;
          
          if(txSender.equals(x.sender))
          {
            x.assetsA.add(assetInfo);
            lookup.put(hashdata, x);
          }
          else if(txSender.equals(x.recipient))
          {
            x.assetsB.add(assetInfo);
            lookup.put(hashdata, x);
          }
          
          continue;
        }
        
        //unsupported tx
        continue;
      }
      catch(Exception e)
      {
        Logger.logMessage("Failed to parse tx");
      }
    }
    
    for(String k : lookup.keySet())
    {
      SwapInfo a = lookup.get(k);
      if(a == null) continue;
      result.add(a);
    }
    return result;
  }
  
  boolean isQuack(JSONObject message)
  {
    if(!message.containsKey("quack")) return false;
    Long v = Convert.nullToZero((Long) message.get("quack"));
    if(v != 1) return false;
    return true;
  }
  
  boolean isTrigger(JSONObject message)
  {
    if(!message.containsKey("trigger")) return false;
    Long v = Convert.nullToZero((Long) message.get("trigger"));
    if(v != 1) return false;
    return true;
  }
  
  String getSwapid(JSONObject message)
  {
    if(!message.containsKey("swapid")) return null;
    String v = Convert.emptyToNull((String) message.get("swapid"));
    return v;
  }
  
  @SuppressWarnings("unchecked")
  JSONObject phasedPayment(String recipient, String secretPhrase, String fullHash, String swapid,
      int deadline, long finishheight, long payment, String message) throws NxtApiException
  {
    JSONObject messageJson = new JSONObject();
    messageJson.put("quack", 1L);
    messageJson.put("message", message);
    messageJson.put("swapid", swapid);
    
    JSONObject paytx = api.createPhasedPayment(recipient, secretPhrase, fullHash, deadline, finishheight, payment, messageJson.toString());
    return paytx;
  }
  
  @SuppressWarnings("unchecked")
  JSONObject phasedAsset(String recipient, String secretPhrase, String fullHash, String swapid,
      int deadline, long finishheight, String assetId, long payment, String message) throws NxtApiException
  {
    JSONObject messageJson = new JSONObject();
    messageJson.put("quack", 1L);
    messageJson.put("message", message);
    messageJson.put("swapid", swapid);
    
    JSONObject paytx = api.createPhasedAsset(recipient, secretPhrase, fullHash, deadline, finishheight, assetId, payment, messageJson.toString());
    return paytx;
  }
}
