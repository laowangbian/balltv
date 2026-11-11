package cn.balltv.portal;
import java.util.*;import java.text.*;

/** Editorial priorities for Chinese viewers, never a fabricated audience count. */
public final class EventOrder {
 static final String[][] LEAGUES={
  {"NBA","155","nba|美国职业篮球|美职篮"},{"CBA","140","cba|中国男子篮球职业|中国男子职业篮球"},
  {"世界杯","160","世界杯|world cup"},{"欧洲杯","150","欧洲杯|european championship"},{"欧冠","145","欧冠|欧洲冠军联赛|champions league"},{"中超","135","中超|中国足球超级联赛"},{"英超","135","英超|英格兰超级联赛|英格兰足球超级联赛"},{"西甲","130","西甲|西班牙甲级联赛|西班牙足球甲级联赛"},{"意甲","125","意甲|意大利甲级联赛|意大利足球甲级联赛"},{"德甲","125","德甲|德国甲级联赛|德国足球甲级联赛"},{"法甲","120","法甲|法国甲级联赛|法国足球甲级联赛"},{"亚洲杯","135","亚洲杯|asian cup"},{"世俱杯","135","世俱杯"},{"奥运会","145","奥运|olympic"},{"亚运会","120","亚运"},{"欧联","110","欧联|欧罗巴"},{"WNBA","100","wnba|美国女子职业篮球"},{"WCBA","100","wcba|中国女子篮球"},{"中甲","100","中甲|中国足球甲级联赛"},{"网球大满贯","115","温布尔登|温网|澳网|法网|美国网球公开|美网"},{"斯诺克世锦赛","110","斯诺克世界锦标|斯诺克世锦|世界斯诺克"},{"WTT","105","wtt|乒乓球世界杯|乒乓球世界锦标"},{"英雄联盟全球赛","125","英雄联盟全球|英雄联盟世界|lol worlds"},{"LPL","110","lpl"},{"KPL","105","kpl"},{"DOTA2国际邀请赛","115","国际邀请赛|the international"}
 };
 static String[] rule(String raw){String s=raw==null?"":raw.toLowerCase(Locale.ROOT).replace(" ","");for(String[] rule:LEAGUES){if(rule[0].equals("NBA")&&(s.contains("wnba")||s.contains("ncaa")))continue;if(rule[0].equals("CBA")&&s.contains("wcba"))continue;for(String alias:rule[2].split("\\|"))if(s.contains(alias))return rule;}return null;}
 static boolean verified(Feeds.Match m){return !m.home.isEmpty()&&!m.away.isEmpty()&&(!m.estimated||Events.epoch(m.time)>0);}
 static boolean chineseTeam(Feeds.Match m){return m.home.contains("中国")||m.away.contains("中国")||m.home.contains("国足")||m.away.contains("国足");}
 static boolean marquee(Feeds.Match m){String teams=m.home+" "+m.away;for(String name:new String[]{"湖人","勇士","凯尔特人","皇家马德里","皇马","巴塞罗那","曼联","曼城","利物浦","阿森纳","拜仁","国际米兰","尤文图斯"})if(teams.contains(name))return true;return false;}
 static int weight(Feeds.Match m){if(!verified(m))return 0;String[] rule=rule(m.league);int score=rule==null?0:Integer.parseInt(rule[1]);if(m.league.matches("(?i).*(U1[0-9]|U2[0-3]|青年|预备队).*"))score=Math.min(score,85);if(m.league.matches(".*(预选|资格|附加|五人制|室内|沙滩).*"))score=Math.min(score,110);if(chineseTeam(m))score=Math.max(score,140);if(score>=100&&marquee(m))score+=14;if(score>=100&&m.league.matches(".*(决赛|季后赛|淘汰赛).*"))score+=12;return score;}
 public static int priority(Events.Group group,Set<String> favorites){int best=0;for(Feeds.Match m:group.members)best=Math.max(best,weight(m));return favorites.contains(group.id)?best+220:best;}
 public static String reason(Events.Group group,Set<String> favorites){if(favorites.contains(group.id))return "你的收藏";Feeds.Match best=null;for(Feeds.Match m:group.members)if(best==null||weight(m)>weight(best))best=m;if(best==null||weight(best)<100)return "";if(chineseTeam(best))return "中国球队 · 重点关注";String[] rule=rule(best.league);if(rule==null)return "";return rule[0]+(marquee(best)?" · 焦点对阵":" · 重点赛事");}
 static int phase(Events.Group group){int phase=2;for(Feeds.Match m:group.members){if(m.status.equals("live"))return 0;if(m.status.equals("upcoming")||m.status.equals("scheduled")||m.status.equals("not_started")||Events.epoch(m.time)>System.currentTimeMillis())phase=1;}return phase;}
 public static String league(Events.Group group){String fallback="其他赛事";for(Feeds.Match m:group.members)if(!m.league.trim().isEmpty()&&!SourceSafety.promotional(m.league)){String[] rule=rule(m.league);boolean detailed=rule!=null&&(rule[0].equals("世界杯")||rule[0].equals("欧洲杯")||rule[0].equals("亚洲杯"))&&m.league.matches("(?i).*(U[0-9]{2}|青年|女子|女足|预选|资格|室内|五人制|沙滩).* ".trim());String label=rule==null||detailed?m.league.trim():rule[0];if(verified(m))return label;fallback=label;}return fallback;}
 static long starts(Events.Group g){return g.start>0?g.start:Long.MAX_VALUE;}
 public static void sort(List<Events.Group> groups,String view,String mode,Set<String> favorites){if(view.equals("history"))return;Collections.sort(groups,(a,b)->{
  if(view.equals("schedule")){int day=dayKey(a.start).compareTo(dayKey(b.start));if(day!=0)return day;if(mode.equals("league")){int l=Integer.compare(leagueWeight(league(b)),leagueWeight(league(a)));if(l!=0)return l;l=league(a).compareTo(league(b));if(l!=0)return l;}}
  else if(mode.equals("recommended")){int p=Integer.compare(phase(a),phase(b));if(p!=0)return p;p=Integer.compare(priority(b,favorites),priority(a,favorites));if(p!=0)return p;int reliability=Integer.compare(b.sourceCount(),a.sourceCount());if(reliability!=0)return reliability;}
  int at=Long.compare(starts(a),starts(b));if(at!=0)return at;int count=Integer.compare(b.sourceCount(),a.sourceCount());return count!=0?count:a.id.compareTo(b.id);
 });}
 static int leagueWeight(String league){String[] r=rule(league);return r==null?0:Integer.parseInt(r[1]);}
 public static String dayKey(long time){if(time<=0)return "9999-99-99";return format("yyyy-MM-dd",time);}
 static final Map<String,String> FORMATS=new LinkedHashMap<String,String>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>2048;}};
 static String format(String pattern,long time){String key=pattern+"|"+time;synchronized(FORMATS){String value=FORMATS.get(key);if(value!=null)return value;}String value=uncachedFormat(pattern,time);synchronized(FORMATS){FORMATS.put(key,value);}return value;}
 static String uncachedFormat(String pattern,long time){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date(time));}
 public static String dayLabel(long time,long now){if(time<=0)return "时间待定";String day=dayKey(time),today=dayKey(now),tomorrow=dayKey(now+86400000L);return (day.equals(today)?"今天 · ":day.equals(tomorrow)?"明天 · ":"")+format("MM月dd日  E",time);}
 public static String dateChip(String day,long now){if(day.equals("9999-99-99"))return "时间待定";long time=Events.epoch(day+" 12:00:00");return (day.equals(dayKey(now))?"今天 ":day.equals(dayKey(now+86400000L))?"明天 ":"")+format("MM/dd",time);}
 public static String time(long time){return time<=0?"待定":format("HH:mm",time);}
 public static String section(Events.Group group,String mode,long now){String label=dayLabel(group.start,now);return mode.equals("league")?label+"  /  "+league(group):label;}
}
