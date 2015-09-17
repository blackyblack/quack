package blackyblack;

import java.util.List;

import nrs.NxtException.NxtApiException;

import org.json.simple.JSONObject;

public interface INxtApi
{
  public Long now();
  
  public String pay(String recipient, String secretPhrase, Long amount,
      String message, String messageEncrypt) throws NxtApiException;
  
  public String tell(String recipient, String secretPhrase,
      String message, String messageEncrypt) throws NxtApiException;
  
  public String payAsset(String recipient, String secretPhrase, Long amount,
      String message, String messageEncrypt, String assetId) throws NxtApiException;
  
  public String readEncryptedMessage(String txid, String secretPhrase) throws NxtApiException;
  
  public List<String> getTransactionIds(String account,
      Boolean pays, Boolean tells,
      int timelimit) throws Exception;
  
  public JSONObject getTransaction(String txid) throws Exception;
  
  public String transactionSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, long amountNQT) throws NxtApiException;
  
  public String assetTransferSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, String assetId, Long assetNQT) throws NxtApiException;
  
  public String getPublicKey(String account) throws NxtApiException;
  
  public JSONObject getBlockchainStatus() throws NxtApiException;
  
  public Long getCurrentBlock() throws NxtApiException;
  
  public JSONObject getAsset(String txid) throws NxtApiException;
  
  public String getUnsignedBytes(JSONObject tx);
  
  public String getFullHash(JSONObject tx);
  
  public JSONObject createPhasedPayment(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, long payment, String message, String encryptedMessage) throws NxtApiException;
  
  public JSONObject createPhasedAsset(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, String assetId, Long qty, String message, String encryptedMessage) throws NxtApiException;
  
  public JSONObject createPhasedMonetary(String recipient, String secretPhrase, String fullHash,
      int deadline, long finishheight, String assetId, Long qty, String message, String encryptedMessage) throws NxtApiException;
  
  public List<JSONObject> getTransactions(String account, int timelimit) throws NxtApiException;
  
  public JSONObject parseTransaction(String data) throws NxtApiException;
}
