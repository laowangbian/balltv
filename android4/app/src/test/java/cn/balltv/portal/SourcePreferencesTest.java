package cn.balltv.portal;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SourcePreferencesTest {
 Feeds.Match match(String source){Feeds.Match m=new Feeds.Match();m.source=source;m.id="fixture";m.home="甲队";m.away="乙队";m.time="2026-11-10 20:00";m.sport="football";return m;}
 @Test public void disabledPeerIsRemovedBeforeGroupingAndFailover(){ArrayList<Feeds.Match> input=new ArrayList<Feeds.Match>(Arrays.asList(match("咖啡"),match("豆包")));ArrayList<Feeds.Match> filtered=SourcePreferences.filter(input,new HashSet<String>(Arrays.asList("咖啡")));assertEquals(1,filtered.size());assertEquals("豆包",Events.group(filtered).get(0).members.get(0).source);assertEquals(2,input.size());}
 @Test public void restoringSourceRevealsRetainedLibraryMembers(){List<Feeds.Match> stored=Arrays.asList(match("咖啡"),match("豆包"));assertEquals(0,SourcePreferences.filter(stored,new HashSet<String>(Arrays.asList("咖啡","豆包"))).size());assertEquals(2,SourcePreferences.filter(stored,Collections.<String>emptySet()).size());}
}
