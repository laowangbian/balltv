package cn.balltv.portal;
import java.util.*;
/** Visible content only; transport callbacks and member arrival order do not redraw cards. */
public final class CardSnapshot {
 static void add(StringBuilder s,Object value){String v=String.valueOf(value);s.append(v.length()).append(':').append(v);}
 public static String capture(String controls,List<Events.Group> groups,Set<String> favorites){StringBuilder s=new StringBuilder();add(s,controls);for(Events.Group g:groups){add(s,g.id);add(s,g.start);add(s,favorites.contains(g.id));ArrayList<Feeds.Match> members=new ArrayList<Feeds.Match>(g.members);Collections.sort(members,(a,b)->a.key().compareTo(b.key()));for(Feeds.Match m:members){for(Object v:new Object[]{m.key(),m.title,m.home,m.away,m.league,m.sport,m.time,m.status,m.score,m.kind,m.estimated})add(s,v);}}return s.toString();}
}
