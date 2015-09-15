package blackyblack.quack;

import org.json.simple.JSONObject;

import nrs.util.Convert;

public class BlockAssetInfo
{
  AssetInfo asset;
  JSONObject tx;
  
  @SuppressWarnings("unchecked")
  public JSONObject toJson()
  {
    JSONObject o = new JSONObject();
    o.put("id", asset.id);
    o.put("QNT", asset.quantity);
    o.put("type", asset.type);
    o.put("confirmations", tx.get("confirmations"));
    JSONObject attach = (JSONObject) tx.get("attachment");
    o.put("finishHeight", attach.get("phasingFinishHeight"));
    o.put("tx", tx);
    return o;
  }
  
  public void fromJson(JSONObject o)
  {
    asset = new AssetInfo();
    asset.id = Convert.emptyToNull((String) o.get("id"));
    asset.quantity = Convert.nullToZero((Long) o.get("NQT"));
    asset.type = Convert.emptyToNull((String) o.get("type"));
    
    if(asset.type == null) asset.type = "NXT";
    asset.decimals = 0;
    asset.name = "";
    
    tx =  (JSONObject) o.get("tx");
  }
}
