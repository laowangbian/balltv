package cn.balltv.portal;
import org.junit.*;import static org.junit.Assert.*;
public class StreamHealthTest {
 Feeds.Match m(){Feeds.Match m=new Feeds.Match();m.source="test";m.id="game";m.title="甲VS乙";m.time="2026-11-09 23:00";return m;}
 Feeds.Line line(){return new Feeds.Line("高清","first","");}
 @Before public void clear(){StreamHealth.GOOD.clear();StreamHealth.BAD.clear();}
 @Test public void mediaEvidenceExpiresAndPlayerFailureInvalidatesGoodLine(){Feeds.Match m=m();Feeds.Line l=line();StreamHealth.accept(m,l,"https://example.org/live.m3u8",new SignalProbe.Result("hls:1:seg.ts",6),100000);assertNotNull(StreamHealth.good(m,190000));assertNull(StreamHealth.good(m,190001));StreamHealth.fail(m,l,130000);assertNull(StreamHealth.good(m,130001));assertTrue(StreamHealth.bad(m,l,140000));assertFalse(StreamHealth.bad(m,l,230001));}
 @Test public void unchangedHlsSequenceDoesNotRemainHealthyForever(){Feeds.Match m=m();Feeds.Line l=line();SignalProbe.Result same=new SignalProbe.Result("hls:1:seg.ts",6);assertTrue(StreamHealth.accept(m,l,"https://example.org/live.m3u8",same,100000));assertTrue(StreamHealth.accept(m,l,"https://example.org/live.m3u8",same,130000));assertFalse(StreamHealth.accept(m,l,"https://example.org/live.m3u8",same,160000));assertNull(StreamHealth.good(m,1180001));}
 @Test public void advancingPlaylistStaysHealthyAndNewFixtureDoesNotReuseOldHealth(){Feeds.Match m=m();Feeds.Line l=line();assertTrue(StreamHealth.accept(m,l,"https://example.org/live.m3u8",new SignalProbe.Result("hls:1:one.ts",6),100000));assertTrue(StreamHealth.accept(m,l,"https://example.org/live.m3u8",new SignalProbe.Result("hls:2:two.ts",6),160000));assertNotNull(StreamHealth.good(m,160001));m.time="2026-11-10 23:00";assertNull(StreamHealth.good(m,160001));}
}
