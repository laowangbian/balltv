package cn.balltv.portal;
import java.util.*;import org.junit.*;import static org.junit.Assert.*;
public class HomeSignalsTest {
 Feeds.Match match(String source){Feeds.Match m=new Feeds.Match();m.source=source;m.id=source;m.home="甲";m.away="乙";m.sport="2";m.time="2026-11-10 15:00";return m;}
 @Before public void clear(){StreamHealth.GOOD.clear();StreamHealth.BAD.clear();}
 @Test public void catalogRowsAndFailedSignalsAreNotEvidence(){Feeds.Match m=match("test");StreamHealth.fail(m,new Feeds.Line("hd","1",""),100000);assertEquals(0,StreamHealth.available(new Events.Group(m),"全部来源",Collections.<String>emptySet(),100001));}
 @Test public void preparationMetadataAloneIsNotEvidence(){Feeds.Match m=match("test");m.status=m.eventState="upcoming";assertEquals(0,StreamHealth.available(new Events.Group(m),"全部来源",Collections.<String>emptySet(),100001));}
 @Test public void playableAlternativeKeepsEventAvailableButRespectsSourceFilterAndRemoval(){Feeds.Match a=match("a"),b=match("b");Events.Group g=new Events.Group(a);g.members.add(b);StreamHealth.playing(b,new Feeds.Line("hd","1",""),"https://example.org/live.m3u8",100000);assertEquals(100000,StreamHealth.available(g,"全部来源",Collections.<String>emptySet(),100001));assertEquals(0,StreamHealth.available(g,"a",Collections.<String>emptySet(),100001));assertEquals(0,StreamHealth.available(g,"全部来源",Collections.singleton("b"),100001));assertEquals(0,StreamHealth.available(g,"全部来源",Collections.<String>emptySet(),190001));}
 @Test public void refreshClearsFailureThrottleWithoutDeletingValidAlternative(){Feeds.Match a=match("a"),b=match("b");Feeds.Line l=new Feeds.Line("hd","1","");StreamHealth.playing(b,l,"https://example.org/live.m3u8",100000);StreamHealth.fail(a,l,100000);StreamHealth.clearFailures();assertFalse(StreamHealth.bad(a,l,100001));assertNotNull(StreamHealth.good(b,100001));}
}
