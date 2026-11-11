package cn.balltv.portal;
import java.util.*;import org.json.*;
/** Match state and room state are deliberately independent. */
public final class LiveState {
 public enum Phase {LIVE,CHECKING,SOON,HIDE}
 static final long MINUTE=60000L;static final Map<String,Ended> ENDED=new LinkedHashMap<String,Ended>();
 static final class Ended{long start,seen;Ended(long s,long t){start=s;seen=t;}}
 public static String words(String value){if(value==null)return "";String s=value.trim().toLowerCase(Locale.ROOT);if(s.matches(".*(已结束|已完赛|比赛结束|完场|已取消|取消比赛|完赛|赛后).* ".trim())||Arrays.asList("finished","ended","ft","aet","cancelled","canceled","closed","complete","completed").contains(s))return "finished";if(s.matches(".*(未开始|未开赛|即将开始|尚未开始|推迟|延期).* ".trim())||Arrays.asList("upcoming","scheduled","not_started","notstarted","pending","deferred","postponed").contains(s))return "upcoming";if(s.matches(".*(上半场|下半场|第一节|第二节|第三节|第四节|加时|中场|进行中|比赛中).* ".trim())||s.equals("live")||s.equals("playing")||s.equals("in_progress"))return "live";return "";}
 public static String matchCode(String sport,int code){if(sport.equals("1")||sport.equals("football")||sport.equals("2")||sport.equals("basketball")){if(code==1)return "upcoming";if(code>=2&&code<=6)return "live";if(code==7||code==9)return "finished";}return "";}
 static long window(String sport){sport=Events.sport(sport);return (sport.equals("football")?210:sport.equals("basketball")?240:sport.equals("tennis")?420:sport.equals("snooker")?600:sport.equals("esports")?360:180)*MINUTE;}
 public static String fixture(Feeds.Match m){if(m.home.isEmpty()||m.away.isEmpty()||m.sport.isEmpty())return "";String a=Events.canonical(m.home),b=Events.canonical(m.away);if(a.equals(b))return "";return Events.sport(m.sport)+"|"+(a.compareTo(b)<0?a+"|"+b:b+"|"+a);}
 static String family(Feeds.Match m){return ExtraSources.handles(m.source)?"kqt-somi":m.source;}
 static boolean hasRoom(Feeds.Match m){JSONObject extra=m.raw==null?null:m.raw.optJSONObject("extra");JSONArray rooms=extra==null?null:extra.optJSONArray("rooms");return rooms!=null?rooms.length()>0:!m.room.isEmpty();}
 static void persist(Feeds.Match m){if(m.raw!=null)try{m.raw.put("_eventState",m.eventState).put("_validatedStart",m.time);}catch(Exception ignored){}}
 static Map<String,List<Feeds.Match>> index(Collection<Feeds.Match> all){Map<String,List<Feeds.Match>> byFixture=new HashMap<String,List<Feeds.Match>>();for(Feeds.Match m:all){String key=fixture(m);List<Feeds.Match> peers=byFixture.get(key);if(peers==null){peers=new ArrayList<Feeds.Match>();byFixture.put(key,peers);}peers.add(m);}return byFixture;}
 public static void prepare(Collection<Feeds.Match> all,long now){Map<String,List<Feeds.Match>> byFixture=index(all);
  // Coffee currently publishes some epochs eight hours early. Correct only
  // when two independent catalog families agree on the same named fixture.
  for(Feeds.Match m:all)if(m.source.equals("咖啡")&&!fixture(m).isEmpty()){long start=Events.epoch(m.time);Map<Long,Set<String>> votes=new HashMap<Long,Set<String>>();for(Feeds.Match peer:byFixture.get(fixture(m))){long at=Events.epoch(peer.time);if(!peer.source.equals("咖啡")&&fixture(m).equals(fixture(peer))&&!peer.eventState.isEmpty()&&Math.abs(at-start-8*3600000L)<2*MINUTE){if(!votes.containsKey(at))votes.put(at,new HashSet<String>());votes.get(at).add(family(peer));}}for(Map.Entry<Long,Set<String>> v:votes.entrySet())if(v.getValue().size()>=2){m.time=String.valueOf(v.getKey());persist(m);break;}}
  synchronized(ENDED){Iterator<Ended> it=ENDED.values().iterator();while(it.hasNext())if(now-it.next().seen>12*3600000L)it.remove();for(Feeds.Match m:all)if(m.eventState.equals("finished")&&!fixture(m).isEmpty()&&Events.epoch(m.time)>0){ENDED.put(fixture(m)+"|"+EventOrder.dayKey(Events.epoch(m.time)),new Ended(Events.epoch(m.time),now));}while(ENDED.size()>256)ENDED.remove(ENDED.keySet().iterator().next());}
 }
 public static Phase phase(Feeds.Match m,Collection<Feeds.Match> all,long now){long start=Events.epoch(m.time);if(!m.kind.equals("live")||start<=0||!hasRoom(m)||fixture(m).isEmpty()||SourceSafety.promotional(m.title)||m.eventState.equals("finished")||now-start>window(m.sport)||start-now>120*MINUTE)return Phase.HIDE;
  String state=m.eventState;String id=fixture(m);synchronized(ENDED){Ended end=ENDED.get(id+"|"+EventOrder.dayKey(start));if(end!=null&&Math.abs(end.start-start)<45*MINUTE&&now-end.seen<12*3600000L)return Phase.HIDE;}
  for(Feeds.Match peer:all)if(id.equals(fixture(peer))&&Math.abs(Events.epoch(peer.time)-start)<45*MINUTE){if(peer.eventState.equals("finished"))return Phase.HIDE;if(peer.eventState.equals("live"))state="live";else if(state.isEmpty()&&peer.eventState.equals("upcoming"))state="upcoming";}
  if(start>now||state.equals("upcoming"))return start>=now-90*MINUTE?Phase.SOON:Phase.CHECKING;
  return state.equals("live")?Phase.LIVE:Phase.CHECKING;
 }
 public static List<Feeds.Match> admit(Collection<Feeds.Match> all,long now){prepare(all,now);Map<String,List<Feeds.Match>> peers=index(all);ArrayList<Feeds.Match> out=new ArrayList<Feeds.Match>();for(Feeds.Match m:all){Phase phase=phase(m,peers.get(fixture(m)),now);if(phase!=Phase.HIDE){m.status=phase==Phase.SOON?"upcoming":phase==Phase.CHECKING?"checking":"live";out.add(m);}}return out;}
 public static boolean finished(Events.Group g,long now){for(Feeds.Match m:g.members){if(m.eventState.equals("finished")||m.status.equals("finished")||m.status.equals("cancelled"))return true;long start=Events.epoch(m.time);synchronized(ENDED){Ended end=ENDED.get(fixture(m)+"|"+EventOrder.dayKey(start));if(end!=null&&Math.abs(end.start-start)<45*MINUTE&&now-end.seen<12*3600000L)return true;}}return false;}
 public static Phase phase(Events.Group g,long now){Phase result=Phase.HIDE;for(Feeds.Match m:g.members){Phase p=phase(m,g.members,now);if(p==Phase.LIVE)return p;if(p==Phase.CHECKING)result=p;else if(p==Phase.SOON&&result==Phase.HIDE)result=p;}return result;}
 public static void sort(List<Events.Group> groups,long now){Collections.sort(groups,(a,b)->Integer.compare(phase(a,now)==Phase.LIVE?0:1,phase(b,now)==Phase.LIVE?0:1));}
 public static String encodeEnded(){synchronized(ENDED){JSONArray a=new JSONArray();for(Map.Entry<String,Ended> e:ENDED.entrySet())try{a.put(new JSONObject().put("key",e.getKey()).put("start",e.getValue().start).put("seen",e.getValue().seen));}catch(Exception ignored){}return a.toString();}}
 public static void restoreEnded(String raw,long now){try{JSONArray a=new JSONArray(raw);synchronized(ENDED){for(int i=0;i<a.length()&&i<256;i++){JSONObject e=a.getJSONObject(i);if(now-e.optLong("seen")<12*3600000L&&e.optLong("seen")<=now)ENDED.put(e.getString("key"),new Ended(e.optLong("start"),e.optLong("seen")));}}}catch(Exception ignored){}}
}
