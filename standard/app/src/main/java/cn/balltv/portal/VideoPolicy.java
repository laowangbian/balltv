package cn.balltv.portal;
import java.util.Locale;

/** No upscaling model and no forced maximum bitrate. Health always wins over labels. */
final class VideoPolicy {
 static int compareLines(int healthA,String a,int healthB,String b,String mode){
  int health=Integer.compare(healthA,healthB);
  return health!=0?health:Integer.compare(labelRank(a,mode),labelRank(b,mode));
 }
 static int labelRank(String value,String mode){
  String name=value==null?"":value.toLowerCase(Locale.ROOT);
  boolean ultra=name.contains("4k")||name.contains("2160")||name.contains("8k");
  boolean high=name.contains("1080")||name.contains("超清")||name.contains("蓝光");
  boolean hd=name.contains("720")||name.contains("高清")||name.contains("hd");
  boolean low=name.contains("标清")||name.contains("流畅")||name.contains("360")||name.contains("480");
  if(ultra)return 5; // Avoid a 4K resource cost just to obtain a nominally sharper line.
  if(mode.equals("smooth"))return hd?0:low?1:high?3:2;
  return high?0:hd?1:low?3:2;
 }
 static boolean enhancementEligible(int sdk,boolean lowRam,int memoryMb,int glVersion){
  return sdk>=21&&!lowRam&&memoryMb>=192&&glVersion>=0x30000;
 }
 static int maxHeight(String mode){return mode.equals("smooth")?720:1080;}
 static int maxBitrate(String mode,boolean lean){return mode.equals("smooth")?4000000:mode.equals("high")?12000000:lean?6000000:10000000;}
}
