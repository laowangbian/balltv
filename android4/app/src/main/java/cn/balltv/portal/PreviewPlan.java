package cn.balltv.portal;
import java.util.*;import java.util.regex.*;
/** HLS previews use a small rendition, complete segment and MP4 initialization. */
final class PreviewPlan {
 final String media,init;final boolean master;
 PreviewPlan(String media,String init,boolean master){this.media=media;this.init=init;this.master=master;}
 static PreviewPlan parse(String url,String text){
  text=text.replace("\uFEFF","").trim();if(!text.startsWith("#EXTM3U")||text.contains("#EXT-X-KEY")&&!text.contains("METHOD=NONE"))return null;
  boolean master=text.contains("#EXT-X-STREAM-INF");ArrayList<String> links=new ArrayList<String>();String init="",best="";long rate=Long.MAX_VALUE,pending=Long.MAX_VALUE;
  for(String row:text.split("\\r?\\n")){row=row.trim();if(row.startsWith("#EXT-X-MAP:")){Matcher m=Pattern.compile("URI=\"([^\"]+)\"").matcher(row);if(m.find())init=Feeds.absolute(url,m.group(1));}if(row.startsWith("#EXT-X-STREAM-INF:")){Matcher m=Pattern.compile("BANDWIDTH=(\\d+)").matcher(row);pending=m.find()?Long.parseLong(m.group(1)):Long.MAX_VALUE;}if(!row.isEmpty()&&!row.startsWith("#")){String link=Feeds.absolute(url,row);links.add(link);if(best.isEmpty()||pending<rate){best=link;rate=pending;}}}
  if(links.isEmpty())return null;return new PreviewPlan(master?best:links.get(Math.max(0,links.size()-2)),init,master);
 }
 static String stableKey(Events.Group group){return "v15:"+group.id;}
}
