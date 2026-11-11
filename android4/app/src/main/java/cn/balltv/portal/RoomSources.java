package cn.balltv.portal;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.*;

/** Curated anonymous sports rooms. No chat, adverts, account or payment APIs. */
public final class RoomSources {
 static final String LONGMEN="龙门",JINGYU="鲸鱼";
 static final String DEVICE=UUID.randomUUID().toString().replace("-","");
 static boolean handles(String source){return LONGMEN.equals(source)||JINGYU.equals(source);}
 static String base(String source){return LONGMEN.equals(source)?"https://xinwaihu.com":SiteMirrors.base(JINGYU);}
 static Object post(String source,String path,JSONObject form)throws Exception{
  Map<String,String> headers=new LinkedHashMap<String,String>();
  if(LONGMEN.equals(source)){headers.put("platform","0");headers.put("lang","zh_cn");headers.put("reg-source","4J621R02");headers.put("device-info",DEVICE);}
  String host=LONGMEN.equals(source)?"https://lmapi.unaofz.cn":base(source);
  JSONObject root=(JSONObject)HttpData.json(HttpData.post(host+path,base(source)+"/",form,headers).body);
  if(root.optInt("status",-1)!=0)throw new IOException("来源接口暂不可用");
  Object data=root.opt("data");
  return data instanceof String&&LONGMEN.equals(source)?decode((String)data):data;
 }
 /** The site's published response envelope, unrelated to video DRM. */
 public static Object decode(String wrapped)throws Exception{
  if(wrapped.length()<9||wrapped.length()>6*1024*1024)throw new IOException("来源响应格式无效");
  String suffix=wrapped.substring(wrapped.length()-8),body=wrapped.substring(0,wrapped.length()-8);
  Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
  cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(("ADKOPSTF%2003.40.51.jpeg"+suffix).getBytes("UTF-8"),"AES"),new IvParameterSpec("ADKOPSTF%2003.40".getBytes("UTF-8")));
  return HttpData.json(new String(cipher.doFinal(HttpData.decode64Bytes(URLDecoder.decode(body,"UTF-8"))),"UTF-8"));
 }
 static JSONArray rooms(String source)throws Exception{
  if(LONGMEN.equals(source))return (JSONArray)post(source,"/api/live/getLiveList",new JSONObject().put("uid",0));
  JSONObject data=(JSONObject)post(source,"/webapi/home/getHomeData",new JSONObject().put("recommend_live_size",100));
  LinkedHashMap<String,JSONObject> unique=new LinkedHashMap<String,JSONObject>();
  for(String key:new String[]{"recommend_lives","hot_lives"}){JSONArray a=data.optJSONArray(key);if(a!=null)for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);unique.put(Feeds.str(r,"anchorid"),r);}}
  return new JSONArray(unique.values());
 }
 static String sport(JSONObject r){String v=Feeds.str(r,"category_id","categoryid","category");if(v.equals("1")||v.equals("2"))return v;String title=Feeds.str(r,"title");if(title.matches("(?i).*(CS2|DOTA|LOL|电竞).*"))return "esports";if(title.matches(".*(斯诺克|台球).*"))return "snooker";return "";}
 public static boolean allowed(JSONObject r){String title=Feeds.str(r,"title");return !Feeds.str(r,"anchorid").isEmpty()&&!sport(r).isEmpty()&&!title.isEmpty()&&!SourceSafety.promotional(title+" "+Feeds.str(r,"anchor_name")+" "+Feeds.str(r.optJSONObject("anchor"),"nick_name"))&&!title.matches(".*(回放|重播|录播).* ".trim())&&r.optInt("ticket")==0&&r.optInt("price")==0&&r.optInt("room_type")==0&&r.optInt("isvideo")==0&&r.optInt("reply_video")==0&&r.optInt("animation")==0&&r.optString("password").isEmpty()&&r.optLong("end_stamp")==0&&(!r.has("status")||r.optInt("status")==1)&&r.optInt("islive",1)==1;}
 static Feeds.Page fetch(String source,String view,int number)throws Exception{
  JSONArray current=rooms(source),events=new JSONArray();
  if(LONGMEN.equals(source))try{events=(JSONArray)post(source,"/api/match/getHotMatchCards4Crawler",new JSONObject());}catch(Exception ignored){}
  return MoreSources.page(parse(source,current,events),number);
 }
 public static List<Feeds.Match> parse(String source,JSONArray rooms,JSONArray events)throws Exception{
  Map<String,JSONObject> matches=new HashMap<String,JSONObject>();for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);matches.put(Feeds.str(e,"match_id","id"),e);}
  LinkedHashMap<String,Feeds.Match> result=new LinkedHashMap<String,Feeds.Match>();
  for(int i=0;i<rooms.length();i++){JSONObject r=rooms.getJSONObject(i);if(!allowed(r))continue;String room=Feeds.str(r,"anchorid"),title=Feeds.str(r,"title"),matchId=Feeds.str(r,"match_id","matchid");JSONObject e=matches.get(matchId);boolean linked=e!=null&&titleMatches(title,e);String id=linked?sport(r)+":"+matchId:"room:"+room;
   Feeds.Match m=result.get(id);if(m==null){m=MoreSources.match(source,id);m.title=title;m.sport=sport(r);m.status="live";m.estimated=!linked;m.room=room;m.cover=Feeds.str(r,"thumb");m.league=Feeds.str(r,"comp");
    if(linked){TeamNames.learn(e);m.home=Feeds.str(e,"home_team_name_zh");m.away=Feeds.str(e,"away_team_name_zh");m.league=Feeds.str(e,"event_name_zh","competition_name_zh");if(m.league.isEmpty())m.league=Feeds.str(r,"comp");m.time=MoreSources.time(e.optLong("time"));m.eventState=LiveState.words(Feeds.str(e,"match_detail_status","status_name"));if(m.eventState.isEmpty()&&e.optInt("match_status",-1)==1)m.eventState="live";m.score=Feeds.str(e,"home_score")+" : "+Feeds.str(e,"away_score");}
    m.raw.put("rooms",new JSONArray());result.put(id,m);}
   String name=Feeds.str(r,"anchor_name");if(name.isEmpty())name=Feeds.str(r.optJSONObject("anchor"),"nick_name");if(name.isEmpty())name="主播 "+room;
   m.raw.getJSONArray("rooms").put(new JSONObject().put("id",room).put("name",name).put("title",title));
  }
  for(Feeds.Match m:result.values())MoreSources.pack(m);return new ArrayList<Feeds.Match>(result.values());
 }
 public static boolean titleMatches(String title,JSONObject event){String home=Feeds.str(event,"home_team_name_zh"),away=Feeds.str(event,"away_team_name_zh"),text=Events.canonical(title);return !home.isEmpty()&&!away.isEmpty()&&text.contains(Events.canonical(home))&&text.contains(Events.canonical(away));}
 static ArrayList<Feeds.Line> lines(Feeds.Match match)throws Exception{
  JSONArray refs=match.raw.getJSONObject("extra").getJSONArray("rooms");ArrayList<Feeds.Line> out=new ArrayList<Feeds.Line>();for(int i=0;i<refs.length();i++){JSONObject r=refs.getJSONObject(i);for(String protocol:LONGMEN.equals(match.source)?new String[]{"hls","flv"}:new String[]{"flv"}){Feeds.Line l=new Feeds.Line(r.getString("name")+" · "+protocol.toUpperCase(Locale.US),r.getString("id")+":"+protocol,"");l.referer=base(match.source)+"/";out.add(l);}}return out;
 }
 static String resolve(Feeds.Match match,Feeds.Line line)throws Exception{
  String[] ids=line.id.split(":");if(ids.length!=2)throw new IOException("无效线路");JSONArray refs=match.raw.getJSONObject("extra").getJSONArray("rooms");JSONObject ref=null;for(int i=0;i<refs.length();i++)if(refs.getJSONObject(i).optString("id").equals(ids[0]))ref=refs.getJSONObject(i);if(ref==null)throw new IOException("缺少当前房间");
  JSONObject data=(JSONObject)post(match.source,"/webapi/live/getLivePageData",new JSONObject().put("anchorid",Long.parseLong(ids[0])).put("uid",0));JSONObject live=data.optJSONObject("live");if(live==null||!allowed(live))throw new IOException("房间已离线、需付费或不符合筛选条件");if(!ref.getString("title").equals(Feeds.str(live,"title")))throw new IOException("房间已切换赛事，请刷新列表");String url=ids[1].equals("hls")?Feeds.str(live,"pull_m3u8_url"):Feeds.str(live,"pull_flv_url","pull_url");if(!StreamResolver.direct(url))throw new IOException("当前未提供此协议的可播放线路");return url;
 }
}
