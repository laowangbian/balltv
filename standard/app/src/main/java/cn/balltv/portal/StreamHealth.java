package cn.balltv.portal;
import java.util.*;
/** Session-only health evidence; signed URLs are never stored in favorites or APK assets. */
public final class StreamHealth {
 public static final class Good {public final Feeds.Line line;public final String url,fingerprint,previewUrl,initUrl;public final long at,unchanged;Good(Feeds.Line l,String u,String f,long a,long same,String p,String i){previewUrl=p;initUrl=i;line=l;url=u;fingerprint=f;at=a;unchanged=same;}}
 static final Map<String,Good> GOOD=new LinkedHashMap<String,Good>();static final Map<String,Long> BAD=new LinkedHashMap<String,Long>();
 static String key(Feeds.Match m){return m.key()+"|"+m.time+"|"+m.label();}static String lineKey(Feeds.Match m,Feeds.Line l){return key(m)+"|"+l.id+"|"+l.name;}
 public static synchronized Good good(Feeds.Match m,long now){Good value=GOOD.get(key(m));return value!=null&&now-value.at<=90000&&now>=value.at?value:null;}
 public static synchronized boolean bad(Feeds.Match m,Feeds.Line l,long now){Long at=BAD.get(lineKey(m,l));return at!=null&&now-at<90000&&now>=at;}
 public static synchronized boolean accept(Feeds.Match m,Feeds.Line l,String url,SignalProbe.Result result,long now){Good previous=GOOD.get(key(m));long same=previous!=null&&previous.fingerprint.equals(result.fingerprint)?previous.unchanged:now;if(!result.fingerprint.equals("flv")&&now-same>Math.max(45000,result.target*4000L)){fail(m,l,now);return false;}ProbeFrames.put(key(m),result.media,now);GOOD.put(key(m),new Good(l,url,result.fingerprint,now,same,result.previewUrl,result.initUrl));BAD.remove(lineKey(m,l));trim(GOOD,128);return true;}
 public static synchronized void playing(Feeds.Match m,Feeds.Line l,String url,long now){GOOD.put(key(m),new Good(l,url,"decoder",now,now,"",""));BAD.remove(lineKey(m,l));trim(GOOD,128);}
 public static synchronized void fail(Feeds.Match m,Feeds.Line l,long now){Good current=GOOD.get(key(m));if(current!=null&&current.line.id.equals(l.id)&&current.line.name.equals(l.name))GOOD.remove(key(m));BAD.put(lineKey(m,l),now);trim(BAD,256);}
 public static synchronized void probeMiss(Feeds.Match m,Feeds.Line l,long now){BAD.put(lineKey(m,l),now);trim(BAD,256);}
 public static synchronized void clearFailures(){BAD.clear();}
 static <T> void trim(Map<String,T> map,int max){while(map.size()>max)map.remove(map.keySet().iterator().next());}
 public static int rank(Feeds.Match m,Feeds.Line l,long now){if(bad(m,l,now))return 2;Good value=good(m,now);return value!=null&&value.line.id.equals(l.id)&&value.line.name.equals(l.name)?0:1;}
 public static synchronized long recentAvailable(Events.Group group,String source,Set<String> disabled,long now,long maxAge){long newest=0;for(Feeds.Match m:group.members){if((!source.equals("全部来源")&&!source.equals(m.source))||disabled.contains(m.source))continue;Good good=GOOD.get(key(m));if(good!=null&&good.at<=now&&now-good.at<maxAge)newest=Math.max(newest,good.at);}return newest;}
 public static long available(Events.Group g,String source,Set<String> disabled,long now){long newest=0;for(Feeds.Match m:g.members)if((source.equals("全部来源")||source.equals(m.source))&&!disabled.contains(m.source)){Good good=good(m,now);if(good!=null)newest=Math.max(newest,good.at);}return newest;}
 public static int verified(Events.Group g,long now){int count=0;for(Feeds.Match m:g.members)if(good(m,now)!=null)count++;return count;}
}
