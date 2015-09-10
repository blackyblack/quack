package nrs;

import java.nio.ByteBuffer;

import nrs.crypto.EncryptedData;
import nrs.util.Convert;

import org.json.simple.JSONObject;

public interface Appendix {

  int getSize();
  void putBytes(ByteBuffer buffer);
  JSONObject getJSONObject();
  byte getVersion();

  static abstract class AbstractAppendix implements Appendix {

      private final byte version;

      AbstractAppendix(JSONObject attachmentData) {
          Long l = (Long) attachmentData.get("version." + getAppendixName());
          version = (byte) (l == null ? 0 : l);
      }

      AbstractAppendix(ByteBuffer buffer, byte transactionVersion) {
          if (transactionVersion == 0) {
              version = 0;
          } else {
              version = buffer.get();
          }
      }

      AbstractAppendix(int version) {
          this.version = (byte) version;
      }

      AbstractAppendix() {
          this.version = 1;
      }

      abstract String getAppendixName();

      @Override
      public final int getSize() {
          return getMySize() + (version > 0 ? 1 : 0);
      }

      abstract int getMySize();

      @Override
      public final void putBytes(ByteBuffer buffer) {
          if (version > 0) {
              buffer.put(version);
          }
          putMyBytes(buffer);
      }

      abstract void putMyBytes(ByteBuffer buffer);

      @SuppressWarnings("unchecked")
      @Override
      public final JSONObject getJSONObject() {
          JSONObject json = new JSONObject();
          if (version > 0) {
              json.put("version." + getAppendixName(), version);
          }
          putMyJSON(json);
          return json;
      }

      abstract void putMyJSON(JSONObject json);

      @Override
      public final byte getVersion() {
          return version;
      }
    }

    public static class Message extends AbstractAppendix {

      public static Message parse(JSONObject attachmentData) throws NxtException.NotValidException {
          if (attachmentData.get("message") == null) {
              return null;
          }
          return new Message(attachmentData);
      }

      private final byte[] message;
      private final boolean isText;

      Message(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
          super(buffer, transactionVersion);
          int messageLength = buffer.getInt();
          this.isText = messageLength < 0; // ugly hack
          if (messageLength < 0) {
              messageLength &= Integer.MAX_VALUE;
          }
          if (messageLength > Constants.MAX_ARBITRARY_MESSAGE_LENGTH) {
              throw new NxtException.NotValidException("Invalid arbitrary message length: " + messageLength);
          }
          this.message = new byte[messageLength];
          buffer.get(this.message);
      }

      Message(JSONObject attachmentData) {
          super(attachmentData);
          String messageString = (String)attachmentData.get("message");
          this.isText = Boolean.TRUE.equals(attachmentData.get("messageIsText"));
          this.message = isText ? Convert.toBytes(messageString) : Convert.parseHexString(messageString);
      }

      public Message(byte[] message) {
          this.message = message;
          this.isText = false;
      }

      public Message(String string) {
          this.message = Convert.toBytes(string);
          this.isText = true;
      }

      @Override
      String getAppendixName() {
          return "Message";
      }

      @Override
      int getMySize() {
          return 4 + message.length;
      }

      @Override
      void putMyBytes(ByteBuffer buffer) {
          buffer.putInt(isText ? (message.length | Integer.MIN_VALUE) : message.length);
          buffer.put(message);
      }

      @SuppressWarnings("unchecked")
      @Override
      void putMyJSON(JSONObject json) {
          json.put("message", isText ? Convert.toString(message) : Convert.toHexString(message));
          json.put("messageIsText", isText);
      }

      public byte[] getMessage() {
          return message;
      }

      public boolean isText() {
          return isText;
      }
  }
    
    public static final class ColoredCoinsAssetTransfer extends AbstractAppendix {

      private final long assetId;
      private final long quantityQNT;
      private final String comment;

      public ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
          super(buffer, transactionVersion);
          this.assetId = buffer.getLong();
          this.quantityQNT = buffer.getLong();
          this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
      }

      public ColoredCoinsAssetTransfer(JSONObject attachmentData) {
          super(attachmentData);
          this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
          this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
          this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
      }

      public ColoredCoinsAssetTransfer(long assetId, long quantityQNT) {
          this.assetId = assetId;
          this.quantityQNT = quantityQNT;
          this.comment = null;
      }

      @Override
      String getAppendixName() {
          return "AssetTransfer";
      }

      @Override
      int getMySize() {
          return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
      }

      @Override
      void putMyBytes(ByteBuffer buffer) {
          buffer.putLong(assetId);
          buffer.putLong(quantityQNT);
          if (getVersion() == 0 && comment != null) {
              byte[] commentBytes = Convert.toBytes(this.comment);
              buffer.putShort((short) commentBytes.length);
              buffer.put(commentBytes);
          }
      }

      @SuppressWarnings("unchecked")
      @Override
      void putMyJSON(JSONObject attachment) {
          attachment.put("asset", Convert.toUnsignedLong(assetId));
          attachment.put("quantityQNT", quantityQNT);
          if (getVersion() == 0) {
              attachment.put("comment", comment);
          }
      }

      public long getAssetId() {
          return assetId;
      }

      public long getQuantityQNT() {
          return quantityQNT;
      }

      public String getComment() {
          return comment;
      }
  }
    
    public static final class ColoredCoinsAssetIssuance extends AbstractAppendix {

      private final String name;
      private final String description;
      private final long quantityQNT;
      private final byte decimals;
      private final String txid;

      public ColoredCoinsAssetIssuance(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
          super(buffer, transactionVersion);
          this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
          this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
          this.quantityQNT = buffer.getLong();
          this.decimals = buffer.get();
          txid = null; //unused
      }

      public ColoredCoinsAssetIssuance(JSONObject attachmentData) {
          super(attachmentData);
          this.name = (String) attachmentData.get("name");
          this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
          this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
          this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
          this.txid = Convert.emptyToNull((String) attachmentData.get("asset"));
      }

      public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals, String txid) {
          this.name = name;
          this.description = Convert.nullToEmpty(description);
          this.quantityQNT = quantityQNT;
          this.decimals = decimals;
          this.txid = txid;
      }

      @Override
      int getMySize() {
          return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
      }

      @Override
      void putMyBytes(ByteBuffer buffer) {
          byte[] name = Convert.toBytes(this.name);
          byte[] description = Convert.toBytes(this.description);
          buffer.put((byte)name.length);
          buffer.put(name);
          buffer.putShort((short) description.length);
          buffer.put(description);
          buffer.putLong(quantityQNT);
          buffer.put(decimals);
      }

      @SuppressWarnings("unchecked")
      @Override
      void putMyJSON(JSONObject attachment) {
          attachment.put("name", name);
          attachment.put("description", description);
          attachment.put("quantityQNT", quantityQNT);
          attachment.put("decimals", decimals);
      }

      public String getName() {
          return name;
      }

      public String getDescription() {
          return description;
      }

      public long getQuantityQNT() {
          return quantityQNT;
      }

      public byte getDecimals() {
          return decimals;
      }
      
      public String getTxid() {
        return txid;
    }

      @Override
      String getAppendixName()
      {
        return "AssetIssuance";
      }
  }

  abstract static class AbstractEncryptedMessage extends AbstractAppendix {

      private final EncryptedData encryptedData;
      private final boolean isText;

      private AbstractEncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
          super(buffer, transactionVersion);
          int length = buffer.getInt();
          this.isText = length < 0;
          if (length < 0) {
              length &= Integer.MAX_VALUE;
          }
          this.encryptedData = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_ENCRYPTED_MESSAGE_LENGTH);
      }

      private AbstractEncryptedMessage(JSONObject attachmentJSON, JSONObject encryptedMessageJSON) {
          super(attachmentJSON);
          byte[] data = Convert.parseHexString((String)encryptedMessageJSON.get("data"));
          byte[] nonce = Convert.parseHexString((String)encryptedMessageJSON.get("nonce"));
          this.encryptedData = new EncryptedData(data, nonce);
          this.isText = Boolean.TRUE.equals(encryptedMessageJSON.get("isText"));
      }

      private AbstractEncryptedMessage(EncryptedData encryptedData, boolean isText) {
          this.encryptedData = encryptedData;
          this.isText = isText;
      }

      @Override
      int getMySize() {
          return 4 + encryptedData.getSize();
      }

      @Override
      void putMyBytes(ByteBuffer buffer) {
          buffer.putInt(isText ? (encryptedData.getData().length | Integer.MIN_VALUE) : encryptedData.getData().length);
          buffer.put(encryptedData.getData());
          buffer.put(encryptedData.getNonce());
      }

      @SuppressWarnings("unchecked")
      @Override
      void putMyJSON(JSONObject json) {
          json.put("data", Convert.toHexString(encryptedData.getData()));
          json.put("nonce", Convert.toHexString(encryptedData.getNonce()));
          json.put("isText", isText);
      }

      public final EncryptedData getEncryptedData() {
          return encryptedData;
      }

      public final boolean isText() {
          return isText;
      }
  }

  public static class EncryptedMessage extends AbstractEncryptedMessage {

      public static EncryptedMessage parse(JSONObject attachmentData) throws NxtException.NotValidException {
          if (attachmentData.get("encryptedMessage") == null ) {
              return null;
          }
          return new EncryptedMessage(attachmentData);
      }

      EncryptedMessage(ByteBuffer buffer, byte transactionVersion) throws NxtException.ValidationException {
          super(buffer, transactionVersion);
      }

      EncryptedMessage(JSONObject attachmentData) throws NxtException.NotValidException {
          super(attachmentData, (JSONObject)attachmentData.get("encryptedMessage"));
      }

      public EncryptedMessage(EncryptedData encryptedData, boolean isText) {
          super(encryptedData, isText);
      }

      @Override
      String getAppendixName() {
          return "EncryptedMessage";
      }

      @SuppressWarnings("unchecked")
      @Override
      void putMyJSON(JSONObject json) {
          JSONObject encryptedMessageJSON = new JSONObject();
          super.putMyJSON(encryptedMessageJSON);
          json.put("encryptedMessage", encryptedMessageJSON);
      }
  }
}
