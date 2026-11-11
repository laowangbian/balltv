package cn.balltv.portal;
import java.util.*;
/** Short-lived schedule discoveries bridge the catalog endpoints without a second UI. */
final class LiveCatalog {
 static final Map<String,Feeds.Match> ROWS=new LinkedHashMap<String,Feeds.Match>();
 static final Map<String,Long> SEEN=new HashMap<String,Long>();
 static boolean supplements(String source){return source.equals("咖啡")||source.equals("豆包")||source.equals("74体育/球吧")||source.equals("178直播");}
 static void remember(Collection<Feeds.Match> rows,long now){for(Feeds.Match m:rows){if(!SourceSafety.usable(m))continue;String key=m.key();ROWS.put(key,m);SEEN.put(key,now);}trim(now);}
 static void trim(long now){Iterator<String> it=ROWS.keySet().iterator();while(it.hasNext()){String key=it.next();long seen=SEEN.get(key);if(now<seen||now-seen>600000){it.remove();SEEN.remove(key);}}while(ROWS.size()>512){String key=ROWS.keySet().iterator().next();ROWS.remove(key);SEEN.remove(key);}}
 static ArrayList<Feeds.Match> recent(long now){trim(now);ArrayList<Feeds.Match> result=new ArrayList<Feeds.Match>();ArrayList<Feeds.Match> all=new ArrayList<Feeds.Match>(ROWS.values());Map<String,List<Feeds.Match>> peers=LiveState.index(all);for(Feeds.Match m:all)if(LiveState.phase(m,peers.get(LiveState.fixture(m)),now)!=LiveState.Phase.HIDE)result.add(m);return result;}
}
