package cn.balltv.portal;
import java.net.*;import java.util.*;import java.io.*;import org.json.*;
/** Only observed, content-checked same-service entries. Never arbitrary host guessing. */
public final class SiteMirrors {
 static final Map<String,List<String>> HOSTS=new LinkedHashMap<String,List<String>>();static final Map<String,String> SELECTED=new HashMap<String,String>();static final Map<String,Long> FAILED=new HashMap<String,Long>();
 static{add("鲸鱼","https://jyu01.com","https://jytv508.com");add("88看球","https://www.88kanqiu.la","https://www.88kanqiu.tw","https://www.88zhibo.me");add("178直播","https://Olympics.178vj2.com","https://supercopa.178zj9.com");add("大米","https://damizhibo.com","https://oneball.live");}
 static void add(String source,String... hosts){HOSTS.put(source,new ArrayList<String>(Arrays.asList(hosts)));}
 static String origin(String url){try{URL u=new URL(url);return u.getProtocol()+"://"+u.getAuthority();}catch(Exception e){return "";}}
 static synchronized String family(String url){String host=origin(url);for(Map.Entry<String,List<String>> e:HOSTS.entrySet())for(String known:e.getValue())if(known.equalsIgnoreCase(host))return e.getKey();return "";}
 public static synchronized String base(String source){String current=SELECTED.get(source);return current!=null?current:HOSTS.get(source).get(0);}
 static synchronized List<String> candidates(String url,long now){String key=family(url);if(key.isEmpty())return Collections.singletonList(url);String origin=origin(url),tail=url.substring(origin.length());ArrayList<String> hosts=new ArrayList<String>();String preferred=SELECTED.get(key);if(preferred!=null)hosts.add(preferred);for(String h:HOSTS.get(key))if(!hosts.contains(h))hosts.add(h);ArrayList<String> ready=new ArrayList<String>(),cool=new ArrayList<String>();for(String h:hosts){Long bad=FAILED.get(h.toLowerCase(Locale.ROOT));(bad!=null&&now-bad<60000?cool:ready).add(h+tail);}ready.addAll(cool);return ready;}
 static boolean readOnly(String url,String data){if(data==null)return true;String f=family(url);try{String path=new URL(url).getPath();return f.equals("鲸鱼")&&(path.equals("/webapi/home/getHomeData")||path.equals("/webapi/live/getLivePageData"))||f.equals("178直播")&&path.matches("/api/web_match/[^/]+/detail/[^/]+");}catch(Exception e){return false;}}
 static String referer(String original,String target){String f=family(target);return !f.isEmpty()&&f.equals(family(original))?origin(target)+original.substring(origin(original).length()):original;}
 static void validate(String url,String body)throws Exception{String f=family(url),path=new URL(url).getPath();if(f.equals("鲸鱼")){JSONObject o=(JSONObject)HttpData.json(body);if(!o.has("status")||!o.has("data"))throw new IOException("备用入口未返回有效目录");}
  else if(f.equals("178直播")&&path.startsWith("/api/")){JSONObject o=(JSONObject)HttpData.json(body);if(!o.has("code")||!o.has("data"))throw new IOException("备用入口未返回有效赛事");}
  else if(f.equals("大米")&&path.endsWith("list.json")){Object o=HttpData.json(body);if(!(o instanceof JSONArray))throw new IOException("备用入口未返回赛事数组");}
  else if(f.equals("88看球")&&path.endsWith("/source")){JSONObject o=(JSONObject)HttpData.json(body);if(!o.has("code")||!o.has("data"))throw new IOException("备用入口未返回直播线路");}
  else if(f.equals("88看球")&&(path.equals("/")||path.isEmpty()||path.equals("/replay"))){org.jsoup.nodes.Document doc=org.jsoup.Jsoup.parse(body);if(doc.select("li.list-group-item").isEmpty())throw new IOException("备用入口未返回赛事列表");}}
 static synchronized void success(String url){String f=family(url);if(!f.isEmpty()){SELECTED.put(f,origin(url));FAILED.remove(origin(url).toLowerCase(Locale.ROOT));}}
 static synchronized void failure(String url,long now){if(!family(url).isEmpty())FAILED.put(origin(url).toLowerCase(Locale.ROOT),now);}
 static synchronized void discovered178(String url){try{URL u=new URL(url);if(!u.getHost().toLowerCase(Locale.ROOT).matches("([a-z0-9-]+\\.)*178[a-z0-9-]*\\.[a-z]{2,}"))return;String host="https://"+u.getAuthority();List<String> list=HOSTS.get("178直播");if(!list.contains(host))list.add(0,host);while(list.size()>4)list.remove(list.size()-1);SELECTED.put("178直播",host);}catch(Exception ignored){}}
 public static synchronized String current(String source){return HOSTS.containsKey(source)?base(source):"";}
}
