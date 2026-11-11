package cn.balltv.portal;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class CardSnapshotTest {
 Feeds.Match match(String source){Feeds.Match m=new Feeds.Match();m.source=source;m.id="1";m.title="a vs b";m.home="a";m.away="b";m.sport="1";m.time="2026-11-10 01:00";m.status="live";return m;}
 @Test public void sameResultsInDifferentSourceArrivalOrderDoNotRedraw(){Feeds.Match a=match("178直播"),b=match("咖啡");Events.Group one=Events.group(Arrays.asList(a,b),false).get(0),two=Events.group(Arrays.asList(b,a),false).get(0);assertEquals(CardSnapshot.capture("live",Arrays.asList(one),Collections.emptySet()),CardSnapshot.capture("live",Arrays.asList(two),Collections.emptySet()));}
 @Test public void scoreAndLeagueChangesAreVisibleUpdates(){Feeds.Match m=match("178直播");Events.Group g=Events.group(Arrays.asList(m),false).get(0);String before=CardSnapshot.capture("live",Arrays.asList(g),Collections.emptySet());m.score="1 : 0";String score=CardSnapshot.capture("live",Arrays.asList(g),Collections.emptySet());assertNotEquals(before,score);m.league="世界杯";assertNotEquals(score,CardSnapshot.capture("live",Arrays.asList(g),Collections.emptySet()));}
 @Test public void filtersAndFavoritesInvalidateVisibleSnapshot(){Feeds.Match m=match("178直播");Events.Group g=Events.group(Arrays.asList(m),false).get(0);String before=CardSnapshot.capture("live",Arrays.asList(g),Collections.emptySet());assertNotEquals(before,CardSnapshot.capture("favorites",Arrays.asList(g),Collections.emptySet()));assertNotEquals(before,CardSnapshot.capture("live",Arrays.asList(g),Collections.singleton(g.id)));}
}

