package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class PreviewPlanTest {
 @Test public void completedSegmentIsPreferredToNewest(){PreviewPlan p=PreviewPlan.parse("https://example.org/a/live.m3u8","#EXTM3U\n#EXTINF:6,\none.ts\n#EXTINF:6,\ntwo.ts\n#EXTINF:6,\nthree.ts");assertEquals("https://example.org/a/two.ts",p.media);}
 @Test public void mp4InitializationIsKept(){PreviewPlan p=PreviewPlan.parse("https://example.org/a/live.m3u8","#EXTM3U\n#EXT-X-MAP:URI=\"init.mp4\"\n#EXTINF:6,\npart.m4s");assertEquals("https://example.org/a/init.mp4",p.init);assertEquals("https://example.org/a/part.m4s",p.media);}
 @Test public void lowBandwidthRenditionAvoidsFullHdPreviewDownload(){PreviewPlan p=PreviewPlan.parse("https://example.org/master.m3u8","#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=6000000\n1080.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=500000\n360.m3u8");assertTrue(p.master);assertEquals("https://example.org/360.m3u8",p.media);}
 @Test public void encryptedAndNonPlaylistResponsesAreNotDecoded(){assertNull(PreviewPlan.parse("https://example.org/live","bad"));assertNull(PreviewPlan.parse("https://example.org/live","#EXTM3U\n#EXT-X-KEY:METHOD=AES-128\nseg.ts"));}
 @Test public void smallStartCorrectionsDoNotDiscardSameMatchPreview(){Feeds.Match m=new Feeds.Match();m.source="咖啡";m.id="one";m.home="甲";m.away="乙";m.sport="football";m.time="2026-11-10 08:00";Events.Group a=new Events.Group(m);m.time="2026-11-10 08:05";Events.Group b=new Events.Group(m);assertEquals(PreviewPlan.stableKey(a),PreviewPlan.stableKey(b));}
}
