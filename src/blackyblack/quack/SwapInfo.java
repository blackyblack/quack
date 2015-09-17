package blackyblack.quack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwapInfo
{
  //what is announced
  public String triggerBytes;
  public String triggerhash;
  public String sender;
  public String recipient;
  public List<AssetInfo> announcedAssets;
  public List<AssetInfo> announcedExpAssets;
  public List<BlockAssetInfo> assetsA;
  public List<BlockAssetInfo> assetsB;
  //what is in blockchain
  public Map<String, List<BlockAssetInfo> > assets;
  public int minFinishHeight;
  public boolean gotTrigger;
  
  public SwapInfo()
  {
    triggerBytes = null;
    triggerhash = null;
    sender = null;
    recipient = null;
    announcedAssets = new ArrayList<AssetInfo>();
    announcedExpAssets = new ArrayList<AssetInfo>();
    assets = new HashMap<String, List<BlockAssetInfo> >();
    assetsA = new ArrayList<BlockAssetInfo>();
    assetsB = new ArrayList<BlockAssetInfo>();
    minFinishHeight = 0;
    gotTrigger = false;
  }
}
