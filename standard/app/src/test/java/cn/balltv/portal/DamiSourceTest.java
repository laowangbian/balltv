package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.util.*;
public class DamiSourceTest {
 String html="<a class='match-card live' data-match-id='1' data-match-time='2026-11-10T02:00:00+08:00' data-match-type='football'><b class='league-badge'>德女联</b><span class='match-status status-live'>直播中</span><div class='team-home'><i class='team-name'>科隆 (女)</i></div><div class='team-away'><i class='team-name'>RB莱比锡 (女)</i></div></a>";
 JSONArray lines()throws Exception{return new JSONArray("[{\"nami_id\":\"1\",\"signals\":[{\"id\":\"a\",\"url\":\"https://example.org/live.m3u8?secret=not_saved\"}]}]");}
 @Test public void metadataAndFemaleTeamNamesAreNormalized()throws Exception{Feeds.Match m=DamiSource.parse(html,lines(),new JSONObject()).get(0);assertEquals("科隆女足",m.home);assertEquals("RB莱比锡女足",m.away);assertEquals("live",m.eventState);assertEquals(Events.epoch("2026-11-10 02:00:00"),Events.epoch(m.time));assertFalse(m.raw.toString().contains("not_saved"));}
 @Test public void missingSignalDoesNotInventPlayableRoom()throws Exception{Feeds.Match m=DamiSource.parse(html,new JSONArray(),new JSONObject()).get(0);assertEquals("",m.room);assertEquals("",m.score);}
 @Test public void explicitFinishedScoreRecordWinsOverOnlineBadge()throws Exception{JSONObject scores=new JSONObject("{\"1\":{\"status\":\"finished\",\"home_score\":2,\"away_score\":1}}");Feeds.Match m=DamiSource.parse(html,lines(),scores).get(0);assertEquals("finished",m.eventState);assertEquals("2 : 1",m.score);}
}
