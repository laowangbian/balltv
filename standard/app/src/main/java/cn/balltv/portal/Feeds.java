package cn.balltv.portal;

import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;

/** Independently implemented source adapters. No upstream project code included. */
public final class Feeds {
    public static final String COFFEE="https://kafeizhibo.cc",DOUBAO="https://www.doubaozhibo.com";
    public static final String UA="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    public static class Match {
        public String source,id,title,league="",home="",away="",score="",time="",status="",cover="",room="",sport="",kind="live";
        public String eventState=""; public boolean estimated; public JSONObject raw; public String key(){return source+":"+kind+":"+id;}
        public String label(){return home.length()>0&&away.length()>0?home+"  vs  "+away:title;}
    }
    public static class Line {public String name,id,url,referer=""; public Line(String n,String i,String u){name=n;id=i;url=u;}}
    public static class Page {public ArrayList<Match> items=new ArrayList<Match>();public boolean more;public String cursor="",note="";}
    public static String base(String source){if(source.equals(BiliEsports.NAME))return BiliEsports.BASE;if(source.equals(HuyaEsports.NAME))return HuyaEsports.BASE;if(source.equals(KuaishouEsports.NAME))return KuaishouEsports.BASE;if(source.equals(HaixingSource.NAME))return HaixingSource.BASE;if(source.equals("咖啡"))return COFFEE;if(source.equals("豆包"))return DOUBAO;return MoreSources.base(source);}
    public static Page fetch(String source,String view,int page,String cursor)throws Exception{if(source.equals(BiliEsports.NAME))return BiliEsports.fetch(view,page);if(source.equals(HuyaEsports.NAME))return HuyaEsports.fetch(view,page);if(source.equals(KuaishouEsports.NAME))return KuaishouEsports.fetch(view,page);if(source.equals(HaixingSource.NAME))return HaixingSource.fetch(view,page);if(SourceCatalog.additional(source))return MoreSources.fetch(source,view,page);Page result=parse(source,view,get(source,path(source,view,page,cursor)));if(source.equals("豆包")&&view.equals("live")){try{Page schedules=parse(source,"schedule",get(source,path(source,"schedule",1,"")));for(Match m:result.items)for(Match s:schedules.items)if(m.id.equals(s.id)){m.home=s.home;m.away=s.away;m.time=s.time;m.score=s.score;m.sport=s.sport;m.raw.put("match",s.raw.optJSONObject("match"));}}catch(Exception ignored){}}return result;}
    public static String absolute(String base,String u){if(u==null||u.isEmpty())return "";try{return new URL(new URL(base),u).toString();}catch(Exception e){return "";}}
    public static String str(JSONObject o,String...keys){if(o==null)return "";for(String key:keys){String s=o.optString(key,"");if(!s.isEmpty()&&!s.equals("null"))return s;}return "";}
    static JSONArray array(JSONObject o,String key){JSONArray a=o.optJSONArray(key);return a==null?new JSONArray():a;}
    public static String path(String source,String view,int page,String cursor){
        if(source.equals("咖啡"))return view.equals("replay")?"/api/v1/recordings?page="+page+"&size=30&type=":view.equals("schedule")?"/api/v1/schedule?type=&page="+page+"&size=30&platform=pc":"/api/v1/archor?type=all";
        if(view.equals("replay"))return "/api/v1/playbacks?category=all&page="+page+"&pageSize=30";
        if(view.equals("schedule")){try{return "/api/v1/home/schedules?days=5&category=all&continuous=true&pageSize=30"+(cursor.isEmpty()?"":"&cursor="+URLEncoder.encode(cursor,"UTF-8"));}catch(Exception e){throw new IllegalArgumentException(e);}}
        return "/api/v1/home/rooms?category=all&page="+page+"&pageSize=30&homeFallback=true";
    }
    public static JSONObject get(String source,String path)throws Exception {return get(source,path,false);}
    static JSONObject get(String source,String path,boolean cache)throws Exception {
        JSONObject o=(JSONObject)HttpData.json(HttpData.get(absolute(base(source),path),base(source)+"/",cache).body);if(o.has("success")&&!o.optBoolean("success"))throw new IOException(source+"："+str(o,"message"));if(o.has("code")&&o.optInt("code")!=0&&o.optInt("code")!=200)throw new IOException(source+"："+str(o,"message"));return o;
    }

    public static String read(InputStream in,int max)throws Exception{try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("响应过大");out.write(b,0,n);}return out.toString("UTF-8");}finally{in.close();}}
    public static Page parse(String source,String view,JSONObject response){
        if(SourceCatalog.additional(source))return MoreSources.restore(source,response);
        Page p=new Page();boolean coffee=source.equals("咖啡");Object data=response.opt("data");JSONArray a=data instanceof JSONArray?(JSONArray)data:data instanceof JSONObject?array((JSONObject)data,"items"):new JSONArray();
        LinkedHashMap<String,Match> unique=new LinkedHashMap<String,Match>();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;Match m=new Match();m.raw=o;m.source=source;m.kind=view.equals("replay")?"replay":"live";
            if(coffee){m.id=str(o,"match_id","id","room_id");m.title=str(o,"title");m.league=str(o,"league_name");m.home=str(o,"home_team");m.away=str(o,"away_team");m.score=str(o,"home_score")+" : "+str(o,"away_score");m.time=str(o,"start_time");m.status=str(o,"status");m.cover=absolute(COFFEE,str(o,"screenshot","cover_image"));m.room=str(o,"room_id");JSONArray rooms=array(o,"archors");if(m.room.isEmpty()&&rooms.length()>0)m.room=str(rooms.optJSONObject(0),"room_id");m.sport=str(o,"type");JSONObject info=o.optJSONObject("match_info");if(m.sport.isEmpty())m.sport=str(info,"type");if(m.title.isEmpty())m.title=m.home+" vs "+m.away;}
            else {JSONObject d=o.optJSONObject(view.equals("replay")?"schedule":"match");if(d==null)d=o;m.id=view.equals("replay")?str(o,"scheduleId"):str(d,"matchId","id");m.title=str(o,"title");m.home=str(d,"home","teamA");m.away=str(d,"away","teamB");m.league=str(d,"league");m.score=str(d,"homeScore","scoreA")+" : "+str(d,"awayScore","scoreB");m.sport=str(d,"sport","dataType");m.time=str(d,"matchTime");int s=d.optInt("status",0);m.status=s==1?"live":s==2?"finished":s==3?"deferred":s==4?"cancelled":"upcoming";m.room=str(o,"url");if(m.room.isEmpty())m.room=str(d,"playUrl");m.room=m.room.replaceFirst("^/play/","");JSONArray rooms=array(o,"rooms");if(m.room.isEmpty()&&rooms.length()>0)m.room=str(rooms.optJSONObject(0),"url").replaceFirst("^/play/","");m.cover=absolute(DOUBAO,str(o,"cover"));if(m.title.isEmpty())m.title=m.home+" vs "+m.away;}
            if(coffee){JSONObject event=o.optJSONObject("match_info");m.eventState=LiveState.words(str(event,"status"));}else {JSONObject event=o.optJSONObject(view.equals("replay")?"schedule":"match");if(event!=null)m.eventState=event.optInt("status",-1)==1?"live":event.optInt("status",-1)==0?"upcoming":event.optInt("status",-1)==2||event.optInt("status",-1)==4?"finished":"";}if(o.has("_eventState"))m.eventState=o.optString("_eventState");if(o.has("_validatedStart"))m.time=o.optString("_validatedStart");if(coffee&&m.time.isEmpty()){JSONObject info=o.optJSONObject("match_info");if(info!=null)m.time=str(info,"match_time");}if(m.score.trim().equals(":"))m.score="";if(m.id.isEmpty())continue;TeamNames.learn(o);if(!unique.containsKey(m.key()))unique.put(m.key(),m);
        }
        p.items.addAll(unique.values());if(data instanceof JSONObject){JSONObject d=(JSONObject)data;p.cursor=d.optString("nextCursor","");p.more=d.has("hasMore")?d.optBoolean("hasMore"):d.optInt("page",1)*d.optInt("pageSize",30)<d.optInt("total",a.length());}else p.more=a.length()>=30;
        if(coffee&&view.equals("live"))p.more=false;return p;
    }
    public static ArrayList<Line> lines(Match m)throws Exception {
        if(m.source.equals(BiliEsports.NAME))return BiliEsports.lines(m);
        if(m.source.equals(HuyaEsports.NAME))return HuyaEsports.lines(m);
        if(m.source.equals(KuaishouEsports.NAME))return KuaishouEsports.lines(m);
        if(m.source.equals(HaixingSource.NAME))return HaixingSource.lines(m);
        if(SourceCatalog.additional(m.source))return MoreSources.lines(m);
        if(m.source.equals("咖啡")){JSONObject data=get(m.source,m.kind.equals("replay")?"/api/v1/match/"+m.id+"/recordings":"/api/v1/room/"+m.room,true).optJSONObject("data");if(data==null)throw new IOException("详情为空");return parseLines(m,data);}
        if(m.kind.equals("replay")){return parseLines(m,m.raw);}
        JSONObject data=get(m.source,"/api/v1/public/signals/"+m.room).optJSONObject("data");if(data==null)throw new IOException("详情为空");return parseLines(m,data);
    }
    public static ArrayList<Line> parseLines(Match m,JSONObject data){ArrayList<Line> result=new ArrayList<Line>();JSONArray a=array(data,m.source.equals("咖啡")?(m.kind.equals("replay")?"replays":"signals"):m.kind.equals("replay")?"lines":"signals");for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String url=str(o,"stream_url","video_url","url","proxyUrl"),id=str(o,"playId","routeId");if(m.source.equals("豆包")&&m.kind.equals("live")){id=id.isEmpty()?url.replaceFirst("^/play/",""):id;url="";}else url=absolute(base(m.source),url);String name=str(o,"name","title");if(url.isEmpty()&&id.isEmpty())continue;result.add(new Line(name.isEmpty()?"线路 "+(i+1):name,id,url));}if(result.isEmpty()&&m.source.equals("豆包")&&!m.room.isEmpty()&&!m.kind.equals("replay"))result.add(new Line("默认线路",m.room,""));return result;}
    public static String resolve(Match m,Line line)throws Exception{
        if(SourceCatalog.esports(m.source))return line.url;
        if(SourceCatalog.additional(m.source))return MoreSources.resolve(m,line);
        if(m.source.equals("豆包")&&m.kind.equals("live")){JSONObject d=get(m.source,"/api/v1/public/signals/"+line.id+"/play").optJSONObject("data");if(d==null||!d.optBoolean("playable",true))throw new IOException("此线路当前不可播放");String u=str(d,"url");if(u.isEmpty())throw new IOException("未返回播放地址");return absolute(DOUBAO,u);}
        return line.url;
    }
}

