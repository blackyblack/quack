package blackyblack.quack;

import java.util.ArrayList;
import java.util.List;

public class SwapInfo
{
  //what is announced
  public String triggerBytes;
  public String triggerPrunnableBytes;
  public String triggerhash;
  public String sender;
  public String recipient;
  public List<AssetInfo> announcedAssets;
  public List<AssetInfo> announcedExpAssets;
  //what is in blockchain
  public List<BlockAssetInfo> assetsA;
  public List<BlockAssetInfo> assetsB;
  public int minFinishHeight;
  public boolean gotTrigger;
  
  public SwapInfo()
  {
    triggerBytes = null;
    triggerPrunnableBytes = null;
    triggerhash = null;
    sender = null;
    recipient = null;
    announcedAssets = new ArrayList<AssetInfo>();
    announcedExpAssets = new ArrayList<AssetInfo>();
    assetsA = new ArrayList<BlockAssetInfo>();
    assetsB = new ArrayList<BlockAssetInfo>();
    minFinishHeight = 0;
    gotTrigger = false;
  }
}
