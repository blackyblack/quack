package blackyblack;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nrs.Appendix;
import nrs.Constants;
import nrs.NxtException.NxtApiException;
import nrs.Transaction;
import nrs.crypto.Crypto;
import nrs.crypto.EncryptedData;
import nrs.util.Convert;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class NxtApi implements INxtApi
{
  private static Long transactionFee = Constants.ONE_NXT;
  public static Object lock = new Object();
  public static String host;
  public static int port;
  
  static {
    host = Application.defaultNrsHost;
    if(Application.getBooleanProperty("blackyblack.isTestnet"))
    {      
      port = Application.defaultTestnetNrsPort;
    }
    else
    {
      port = Application.defaultNrsPort;
    }
  }
  
  public static String api()
  {
    return "http://" + host + ":" + port + "/nxt";
  }
  
  public Long now()
  {
    return (long) Convert.getEpochTime();
  }
  
  public String getPublicKey(String account) throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAccountPublicKey"));
    fields.add(new BasicNameValuePair("account", account));
    
    CloseableHttpResponse response = null;
    String publicKey = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
  
        publicKey = Convert.emptyToNull((String)json.get("publicKey"));
        if(publicKey == null)
        {
          throw new NxtApiException("no publicKey from NRS");
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
    
    return publicKey;
  }
  
  public JSONObject create(String recipient,
      String secretPhrase) throws NxtApiException
  {    
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    String publicString = Convert.toHexString(publicKey);
    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "sendMessage"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("publicKey", publicString));
    fields.add(new BasicNameValuePair("feeNQT", transactionFee.toString()));
    fields.add(new BasicNameValuePair("broadcast", "false"));
    fields.add(new BasicNameValuePair("deadline", "1440"));
    
    CloseableHttpResponse response = null;
    JSONObject tx = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
        tx = (JSONObject) json.get("transactionJSON");
        if(tx == null)
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
    
    return tx;
  }
  
  public String broadcast(String message) throws NxtApiException
  {    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "broadcastTransaction"));
    fields.add(new BasicNameValuePair("transactionBytes", message));
    
    CloseableHttpResponse response = null;
    String txid = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
  
        txid = Convert.emptyToNull((String)json.get("transaction"));
        if(txid == null)
        {
          throw new NxtApiException("no txid from NRS");
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
    
    return txid;
  }
  
  @SuppressWarnings("unchecked")
  public List<String> getTransactionIds(String account,
      Boolean pays, Boolean tells,
      int timelimit) throws Exception
  {
    String messageType = null;
    if(pays && !tells)
    {
      messageType = "0";
    }
    if(tells && !pays)
    {
      messageType = "1";
    }
    
    String timestamp = null;
    if(timelimit > 0)
    {
      if(now() > timelimit)
      {
        timestamp = "" + (now() - timelimit);
      }
    }
    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAccountTransactionIds"));
    fields.add(new BasicNameValuePair("account", account));
    if(messageType != null)
    {
      fields.add(new BasicNameValuePair("type", messageType));
    }
    if(timestamp != null)
    {
      fields.add(new BasicNameValuePair("timestamp", timestamp));
    }
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
    
    JSONObject json = null;
    CloseableHttpResponse response = null;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost http = new HttpPost(api());
    http.setHeader("Origin", host);
    http.setEntity(entity);
    response = httpclient.execute(http);
    HttpEntity result = response.getEntity();
    String content = EntityUtils.toString(result);
      
    JSONParser parser = new JSONParser();
    json = (JSONObject)parser.parse(content);
    JSONArray a = (JSONArray)json.get("transactionIds");
    return (List<String>)a;
  }
  
  public JSONObject getTransaction(String txid) throws Exception
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getTransaction"));
    fields.add(new BasicNameValuePair("transaction", txid));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
    
    JSONObject json = null;
    CloseableHttpResponse response = null;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost http = new HttpPost(api());
    http.setHeader("Origin", host);
    http.setEntity(entity);
    response = httpclient.execute(http);
    HttpEntity result = response.getEntity();
    String content = EntityUtils.toString(result);
      
    JSONParser parser = new JSONParser();
    json = (JSONObject)parser.parse(content);
    if(json == null) return null;
    if(Convert.emptyToNull((String)json.get("transaction")) == null) return null;
    return json;
  } 
  
  @SuppressWarnings("unchecked")
  public String transactionSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, long amountNQT) throws NxtApiException
  {
    JSONObject tempTx = create(recipient, secretPhrase);
    
    String recipientPublicKey = getPublicKey(recipient);
    byte[] recipientPublicKeyBytes = null;
    try
    {
      recipientPublicKeyBytes = Convert.parseHexString(recipientPublicKey);
    }
    catch(NumberFormatException e)
    {
      throw new NxtApiException("bad publicKey from NRS");
    }
    
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    Long senderId = Convert.publicKeyToAccountId(publicKey);
    String encryptedText = "";
    String encryptedNonce = "";
    if(messageEncrypt != null && messageEncrypt.length() > 0)
    {
      EncryptedData encrypted = EncryptedData.encrypt(
          Convert.toBytes(messageEncrypt), Crypto.getPrivateKey(secretPhrase), recipientPublicKeyBytes);
      encryptedText = Convert.toHexString(encrypted.getData());
      encryptedNonce = Convert.toHexString(encrypted.getNonce());
    }
    
    Transaction tx = new Transaction();
    String txid = null;
    
    try
    {
      tx.senderPublicKey = publicKey;
      tx.feeNQT =  transactionFee;
      
      if(amountNQT == 0)
      {
        tx.type = 1;  //AM
      }
      else
      {
        tx.type = 0;
      }
      tx.subtype = 0;
      Long versionValue = Convert.nullToZero((Long) tempTx.get("version"));
      tx.version = versionValue.byteValue();
      tx.ecBlockId = Convert.parseUnsignedLong((String) tempTx.get("ecBlockId"));
      tx.ecBlockHeight = ((Long) tempTx.get("ecBlockHeight")).intValue();
      
      JSONObject attachmentData = new JSONObject();
      if(message != null && message.length() > 0)
      {
        attachmentData.put("version.Message", 1L);
        attachmentData.put("messageIsText", true);
        attachmentData.put("message", message);
      }
      
      if(messageEncrypt != null && messageEncrypt.length() > 0)
      {
        JSONObject encryptedData = new JSONObject();
        encryptedData.put("data", encryptedText);
        encryptedData.put("nonce", encryptedNonce);
        encryptedData.put("isText", true);
        attachmentData.put("encryptedMessage", encryptedData);
        attachmentData.put("version.EncryptedMessage", 1L);
      }
      
      tx.message = Appendix.Message.parse(attachmentData);
      tx.encryptedMessage = Appendix.EncryptedMessage.parse(attachmentData);
      
      List<Appendix.AbstractAppendix> list = new ArrayList<>();
      if (tx.message != null) {
          list.add(tx.message);
      }
      if (tx.encryptedMessage != null) {
          list.add(tx.encryptedMessage);
      }
     
      tx.appendages = Collections.unmodifiableList(list);
      int appendagesSize = 0;
      for (Appendix appendage : tx.appendages) {
          appendagesSize += appendage.getSize();
      }
      tx.appendagesSize = appendagesSize;
      
      tx.amountNQT = amountNQT;
      tx.senderId = senderId;
      tx.recipientId = Convert.parseAccountId(recipient);
      tx.deadline = 1440;
      tx.timestamp = ((Long) tempTx.get("timestamp")).intValue();
      tx.height = ((Long) tempTx.get("height")).intValue();
      
      byte[] unsignedTx = tx.getBytes();
      byte[] signedTx = Crypto.sign(unsignedTx, secretPhrase);
      tx.signature = signedTx;
      txid = broadcast(Convert.toHexString(tx.getBytes()));
    }
    catch(Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    return txid;
  }
  
  @SuppressWarnings("unchecked")
  public String assetTransferSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, String assetId, Long assetNQT) throws NxtApiException
  {
    JSONObject tempTx = create(recipient, secretPhrase);
    
    String recipientPublicKey = getPublicKey(recipient);
    byte[] recipientPublicKeyBytes = null;
    try
    {
      recipientPublicKeyBytes = Convert.parseHexString(recipientPublicKey);
    }
    catch(NumberFormatException e)
    {
      throw new NxtApiException("bad publicKey from NRS");
    }
    
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    Long senderId = Convert.publicKeyToAccountId(publicKey);
    String encryptedText = "";
    String encryptedNonce = "";
    if(messageEncrypt != null && messageEncrypt.length() > 0)
    {
      EncryptedData encrypted = EncryptedData.encrypt(
          Convert.toBytes(messageEncrypt), Crypto.getPrivateKey(secretPhrase), recipientPublicKeyBytes);
      encryptedText = Convert.toHexString(encrypted.getData());
      encryptedNonce = Convert.toHexString(encrypted.getNonce());
    }
    
    Transaction tx = new Transaction();
    String txid = null;
    
    try
    {
      tx.senderPublicKey = publicKey;
      tx.feeNQT =  transactionFee;
      tx.type = 2;  //Assets
      tx.subtype = 1; //Asset Transfer
      Long versionValue = Convert.nullToZero((Long) tempTx.get("version"));
      tx.version = versionValue.byteValue();
      tx.ecBlockId = Convert.parseUnsignedLong((String) tempTx.get("ecBlockId"));
      tx.ecBlockHeight = ((Long) tempTx.get("ecBlockHeight")).intValue();
      
      JSONObject attachmentData = new JSONObject();
      
      if(message != null && message.length() > 0)
      {
        attachmentData.put("version.Message", 1L);
        attachmentData.put("messageIsText", true);
        attachmentData.put("message", message);
      }
      
      if(messageEncrypt != null && messageEncrypt.length() > 0)
      {
        JSONObject encryptedData = new JSONObject();
        encryptedData.put("data", encryptedText);
        encryptedData.put("nonce", encryptedNonce);
        encryptedData.put("isText", true);
        attachmentData.put("encryptedMessage", encryptedData);
        attachmentData.put("version.EncryptedMessage", 1L);
      }
      
      tx.message = Appendix.Message.parse(attachmentData);
      tx.encryptedMessage = Appendix.EncryptedMessage.parse(attachmentData);
      
      List<Appendix.AbstractAppendix> list = new ArrayList<>();
      list.add(new Appendix.ColoredCoinsAssetTransfer(Convert.parseUnsignedLong(assetId), assetNQT));
      if (tx.message != null) {
          list.add(tx.message);
      }
      if (tx.encryptedMessage != null) {
          list.add(tx.encryptedMessage);
      }
     
      tx.appendages = Collections.unmodifiableList(list);
      int appendagesSize = 0;
      for (Appendix appendage : tx.appendages) {
          appendagesSize += appendage.getSize();
      }
      tx.appendagesSize = appendagesSize;
      
      tx.amountNQT = 0;
      tx.senderId = senderId;
      tx.recipientId = Convert.parseAccountId(recipient);
      tx.deadline = 1440;
      tx.timestamp = ((Long) tempTx.get("timestamp")).intValue();
      tx.height = ((Long) tempTx.get("height")).intValue();
      
      byte[] unsignedTx = tx.getBytes();
      byte[] signedTx = Crypto.sign(unsignedTx, secretPhrase);
      tx.signature = signedTx;
      txid = broadcast(Convert.toHexString(tx.getBytes()));
    }
    catch(Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    return txid;
  }
  
  public String pay(String recipient, String secretPhrase, Long amount,
      String message, String messageEncrypt) throws NxtApiException
  {
    return transactionSafe(recipient, secretPhrase, message, messageEncrypt, amount);
  }
  
  public String tell(String recipient, String secretPhrase,
      String message, String messageEncrypt) throws NxtApiException
  {
    return transactionSafe(recipient, secretPhrase, message, messageEncrypt, 0);
  }
  
  public String payAsset(String recipient, String secretPhrase, Long amount,
      String message, String messageEncrypt, String assetId) throws NxtApiException
  {
    return assetTransferSafe(recipient, secretPhrase, message, messageEncrypt, assetId, amount);
  }
  
  public String readEncryptedMessageSafe(String txid, String secretPhrase) throws NxtApiException
  {
    JSONObject o = null;
    try
    {
      o = getTransaction(txid);
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    if(o == null) return "";
    
    String sender = (String) o.get("senderRS");
    if(sender == null) return "";
    
    String senderPublicKey = getPublicKey(sender);
    
    JSONObject attach = (JSONObject) o.get("attachment");
    if(attach == null) return "";
    
    JSONObject encryptedMessage = (JSONObject) attach.get("encryptedMessage");
    if(encryptedMessage == null) return "";
    
    String data = (String) encryptedMessage.get("data");
    String nonce = (String) encryptedMessage.get("nonce");
    
    if(data == null || nonce == null) return "";
    
    EncryptedData enc = null;
    byte[] theirPublicKey = null;
    try
    {
      enc = new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      theirPublicKey = Convert.parseHexString(senderPublicKey);
    }
    catch (Exception e)
    {
    }
    
    if(enc == null || theirPublicKey == null) return "";
    
    byte[] result = enc.decrypt(Crypto.getPrivateKey(secretPhrase), theirPublicKey);
    return Convert.toString(result);
  }
  
  public String readEncryptedMessage(String txid, String secretPhrase) throws NxtApiException
  {
    return readEncryptedMessageSafe(txid, secretPhrase);
  }
  
  public JSONObject getBlockchainStatus() throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getBlockchainStatus"));
    
    JSONObject answer = null;
    CloseableHttpResponse response = null;
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
        answer = (JSONObject)parser.parse(content);
      }
      catch(ConnectException e)
      {
      }
      catch(ClientProtocolException e)
      {
      }
      catch(IOException e)
      {
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
    
    return answer;
  }
  
  public Long getCurrentBlock() throws NxtApiException
  {
    JSONObject status = getBlockchainStatus();
    
    Long blocksNow = Convert.nullToZero((Long) status.get("numberOfBlocks"));
    return blocksNow;
  }
  
  public JSONObject getAsset(String txid) throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAsset"));
    fields.add(new BasicNameValuePair("asset", txid));
    fields.add(new BasicNameValuePair("includeCounts", "false"));
    
    CloseableHttpResponse response = null;
    JSONObject json = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        json = (JSONObject)parser.parse(content);
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
  
  public String getUnsignedBytes(JSONObject tx)
  {
    return Convert.emptyToNull((String) tx.get("unsignedTransactionBytes"));
  }
  
  public String getFullHash(JSONObject tx)
  {
    return Convert.emptyToNull((String) tx.get("fullHash"));
  }
  
  public JSONObject createPhasedPayment(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, long payment, String message, String encryptedMessage) throws NxtApiException
  {    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "sendMoney"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("secretPhrase", secretPhrase));
    fields.add(new BasicNameValuePair("feeNQT", "" + 2 * Constants.ONE_NXT));
    fields.add(new BasicNameValuePair("broadcast", "true"));
    fields.add(new BasicNameValuePair("deadline", "" + deadline));
    fields.add(new BasicNameValuePair("amountNQT", "" + payment));
    fields.add(new BasicNameValuePair("message", message));
    fields.add(new BasicNameValuePair("messageIsText", "true"));
    fields.add(new BasicNameValuePair("messageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("messageToEncrypt", encryptedMessage));
    fields.add(new BasicNameValuePair("messageToEncryptIsText", "true"));
    fields.add(new BasicNameValuePair("encryptedMessageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("phased", "true"));
    fields.add(new BasicNameValuePair("phasingFinishHeight", "" + finishheight));
    fields.add(new BasicNameValuePair("phasingVotingModel", "4"));
    fields.add(new BasicNameValuePair("phasingQuorum", "1"));
    fields.add(new BasicNameValuePair("phasingLinkedFullHash", fullHash));    
    
    CloseableHttpResponse response = null;
    JSONObject tx = null;
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
        JSONObject json = (JSONObject)parser.parse(content);
        tx = (JSONObject) json.get("transactionJSON");
        if(tx == null)
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
    
    return tx;
  }
  
  public JSONObject createPhasedAsset(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, String assetId, Long qty, String message, String encryptedMessage) throws NxtApiException
  {    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "transferAsset"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("secretPhrase", secretPhrase));
    fields.add(new BasicNameValuePair("feeNQT", "" + 2 * Constants.ONE_NXT));
    fields.add(new BasicNameValuePair("broadcast", "true"));
    fields.add(new BasicNameValuePair("deadline", "" + deadline));
    fields.add(new BasicNameValuePair("asset", assetId)); 
    fields.add(new BasicNameValuePair("quantityQNT", "" + qty));
    fields.add(new BasicNameValuePair("message", message));
    fields.add(new BasicNameValuePair("messageIsText", "true"));
    fields.add(new BasicNameValuePair("messageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("messageToEncrypt", encryptedMessage));
    fields.add(new BasicNameValuePair("messageToEncryptIsText", "true"));
    fields.add(new BasicNameValuePair("encryptedMessageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("phased", "true"));
    fields.add(new BasicNameValuePair("phasingFinishHeight", "" + finishheight));
    fields.add(new BasicNameValuePair("phasingVotingModel", "4"));
    fields.add(new BasicNameValuePair("phasingQuorum", "1"));
    fields.add(new BasicNameValuePair("phasingLinkedFullHash", fullHash));    
    
    CloseableHttpResponse response = null;
    JSONObject tx = null;
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
        JSONObject json = (JSONObject)parser.parse(content);
        tx = (JSONObject) json.get("transactionJSON");
        if(tx == null)
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
    
    return tx;
  }
  
  public JSONObject createPhasedMonetary(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, String assetId, Long qty, String message, String encryptedMessage) throws NxtApiException
  {    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "transferCurrency"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("secretPhrase", secretPhrase));
    fields.add(new BasicNameValuePair("feeNQT", "" + 2 * Constants.ONE_NXT));
    fields.add(new BasicNameValuePair("broadcast", "true"));
    fields.add(new BasicNameValuePair("deadline", "" + deadline));
    fields.add(new BasicNameValuePair("currency", assetId)); 
    fields.add(new BasicNameValuePair("units", "" + qty));
    fields.add(new BasicNameValuePair("message", message));
    fields.add(new BasicNameValuePair("messageIsText", "true"));
    fields.add(new BasicNameValuePair("messageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("messageToEncrypt", encryptedMessage));
    fields.add(new BasicNameValuePair("messageToEncryptIsText", "true"));
    fields.add(new BasicNameValuePair("encryptedMessageIsPrunable", "true"));
    fields.add(new BasicNameValuePair("phased", "true"));
    fields.add(new BasicNameValuePair("phasingFinishHeight", "" + finishheight));
    fields.add(new BasicNameValuePair("phasingVotingModel", "4"));
    fields.add(new BasicNameValuePair("phasingQuorum", "1"));
    fields.add(new BasicNameValuePair("phasingLinkedFullHash", fullHash));    
    
    CloseableHttpResponse response = null;
    JSONObject tx = null;
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
        JSONObject json = (JSONObject)parser.parse(content);
        tx = (JSONObject) json.get("transactionJSON");
        if(tx == null)
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
    
    return tx;
  }
  
  @SuppressWarnings("unchecked")
  public List<JSONObject> getTransactions(String account, int timelimit) throws NxtApiException
  {    
    String timestamp = null;
    JSONArray a = new JSONArray();
    
    if(timelimit > 0)
    {
      if(now() > timelimit)
      {
        timestamp = "" + (now() - timelimit);
      }
    }
    
    try
    {
      List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
      fields.add(new BasicNameValuePair("requestType", "getBlockchainTransactions"));
      fields.add(new BasicNameValuePair("account", account));
      if(timestamp != null)
      {
        fields.add(new BasicNameValuePair("timestamp", timestamp));
      }
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
      
      JSONObject json = null;
      CloseableHttpResponse response = null;
      try
      {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
          
        JSONParser parser = new JSONParser();
        json = (JSONObject)parser.parse(content);
        a = (JSONArray)json.get("transactions");
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
    return a;
  }
  
  public JSONObject parseTransaction(String data) throws NxtApiException
  {    
    JSONObject json = null;
    
    try
    {
      List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
      fields.add(new BasicNameValuePair("requestType", "parseTransaction"));
      fields.add(new BasicNameValuePair("transactionBytes", data));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
      
      
      CloseableHttpResponse response = null;
      try
      {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
          
        JSONParser parser = new JSONParser();
        json = (JSONObject)parser.parse(content);
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
