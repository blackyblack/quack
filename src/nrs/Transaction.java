package nrs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.json.simple.JSONObject;

import nrs.util.Convert;
import nrs.util.Logger;

public class Transaction {

  public short deadline;
  public byte[] senderPublicKey;
  public long amountNQT;
  public long feeNQT;
  public byte type;
  public byte subtype;
  public byte version;
  public int timestamp;

  public long recipientId;
  public long senderId;
  public String referencedTransactionFullHash;
  public byte[] signature;
  public Appendix.Message message;
  public Appendix.EncryptedMessage encryptedMessage;
  public Appendix.ColoredCoinsAssetTransfer assetTransfer;
  public long blockId;
  public int height = Integer.MAX_VALUE;
  public long id;
  public String senderRS;
  public int blockTimestamp = -1;
  public String fullHash;
  public int ecBlockHeight;
  public long ecBlockId;
  public boolean phased = false;
  
  public List<? extends Appendix.AbstractAppendix> appendages;
  public int appendagesSize;
  
  private int getFlags() {
    int flags = 0;
    int position = 1;
    if (message != null) {
        flags |= position;
    }
    position <<= 1;
    if (encryptedMessage != null) {
        flags |= position;
    }
    position <<= 1;
    position <<= 1;
    return flags;
}
  
  private int signatureOffset() {
    return 1 + 1 + 4 + 2 + 32 + 8 + (8 + 8 + 32);
}
  
  int getSize() {
    return signatureOffset() + 64  + (version > 0 ? 4 + 4 + 8 : 0) + appendagesSize;
  }
  
  public byte[] getBytes()
  {
    try {
        ByteBuffer buffer = ByteBuffer.allocate(getSize());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(type);
        buffer.put((byte) ((version << 4) | subtype));
        buffer.putInt(timestamp);
        buffer.putShort(deadline);
        buffer.put(senderPublicKey);
        buffer.putLong(recipientId);
        buffer.putLong(amountNQT);
        buffer.putLong(feeNQT);
        if (referencedTransactionFullHash != null) {
            buffer.put(Convert.parseHexString(referencedTransactionFullHash));
        } else {
            buffer.put(new byte[32]);
        }
        
        buffer.put(signature != null ? signature : new byte[64]);
        if (version > 0) {
            buffer.putInt(getFlags());
            buffer.putInt(ecBlockHeight);
            buffer.putLong(ecBlockId);
        }
        for (Appendix appendage : appendages) {
            appendage.putBytes(buffer);
        }
        return buffer.array();
    } catch (RuntimeException e) {
        Logger.logDebugMessage("Failed to get transaction bytes for transaction");
        throw e;
    }
  }
  
  public static Transaction parseTransaction(JSONObject transactionData) throws NxtException.NotValidException
  {
    Transaction tx = new Transaction();
    try
    {
      tx.id = Convert.parseUnsignedLong((String) transactionData.get("transaction"));
      tx.type = ((Long) transactionData.get("type")).byteValue();
      tx.subtype = ((Long) transactionData.get("subtype")).byteValue();
      tx.timestamp = ((Long) transactionData.get("timestamp")).intValue();
      tx.height = ((Long) transactionData.get("height")).intValue();
      tx.deadline = ((Long) transactionData.get("deadline")).shortValue();
      tx.senderPublicKey = Convert.parseHexString((String) transactionData.get("senderPublicKey"));
      tx.amountNQT = Convert.parseLong(transactionData.get("amountNQT"));
      tx.feeNQT = Convert.parseLong(transactionData.get("feeNQT"));
      tx.referencedTransactionFullHash = (String) transactionData.get("referencedTransactionFullHash");
      tx.signature = Convert.parseHexString((String) transactionData.get("signature"));
      Long versionValue = (Long) transactionData.get("version");
      tx.version = versionValue == null ? 0 : versionValue.byteValue();
      JSONObject attachmentData = (JSONObject) transactionData.get("attachment");
        
      tx.ecBlockHeight = 0;
      tx.ecBlockId = 0;
      if (tx.version > 0)
      {
        tx.ecBlockHeight = ((Long) transactionData.get("ecBlockHeight")).intValue();
        tx.ecBlockId = Convert.parseUnsignedLong((String) transactionData.get("ecBlockId"));
      }

      tx.recipientId = Convert.parseUnsignedLong((String) transactionData.get("recipient"));
      tx.senderId = Convert.parseUnsignedLong((String) transactionData.get("sender"));
      tx.senderRS = Convert.rsAccount(tx.senderId);
      
      tx.assetTransfer = null;
      if (attachmentData != null)
      {
        tx.message = Appendix.Message.parse(attachmentData);
        tx.encryptedMessage = Appendix.EncryptedMessage.parse(attachmentData);
        
        if(tx.type == 2 && tx.subtype == 1)
        {
          tx.assetTransfer = new Appendix.ColoredCoinsAssetTransfer(attachmentData);
        }
      }
      return tx;
    }
    catch (NxtException.NotValidException|RuntimeException e)
    {
        Logger.logDebugMessage("Failed to parse transaction: " + transactionData.toJSONString());
        throw e;
    }
}
}