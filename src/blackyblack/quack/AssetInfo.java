package blackyblack.quack;

import org.json.simple.JSONObject;

import nrs.util.Convert;

public class AssetInfo
{
  public String name;
  public String id;
  public String type;
  public long quantity;
  public long decimals;
  
  @SuppressWarnings("unchecked")
  public JSONObject toJson()
  {
	  JSONObject o = new JSONObject();
	  o.put("id", id);
	  o.put("QNT", quantity);
	  o.put("type", type);
	  return o;
  }
  
  public void fromJson(JSONObject o)
  {
	  id = Convert.emptyToNull((String) o.get("id"));
	  quantity = Convert.nullToZero((Long) o.get("QNT"));
	  type = Convert.emptyToNull((String) o.get("type"));
	  
	  if(type == null) type = "NXT";
	  decimals = 0;
	  name = "";
  }
}
