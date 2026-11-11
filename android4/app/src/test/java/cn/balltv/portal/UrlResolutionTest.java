package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class UrlResolutionTest {
 @Test public void relativeSegmentsUsePlaylistDirectory(){assertEquals("https://cdn.example/live/segment.ts",Feeds.absolute("https://cdn.example/live/index.m3u8","segment.ts"));}
 @Test public void relativeSegmentsDoNotTreatSignedPlaylistAsDirectory(){assertEquals("https://cdn.example/live/a%2Bb.ts?sig=one%2Ftwo",Feeds.absolute("https://cdn.example/live/index.m3u8?token=x%2Fy","a%2Bb.ts?sig=one%2Ftwo"));}
 @Test public void originAndAbsolutePathsStillWork(){assertEquals("https://site.example/api/list",Feeds.absolute("https://site.example","api/list"));assertEquals("https://other.example/a",Feeds.absolute("https://site.example/page.html","//other.example/a"));}
}
