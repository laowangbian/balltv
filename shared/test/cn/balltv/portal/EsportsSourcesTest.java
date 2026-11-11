package cn.balltv.portal;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class EsportsSourcesTest {
    @Test public void sourcesOnlyAppearInTheirOwnSection() {
        assertTrue(SourceCatalog.inCategory("看球通", "全部"));
        assertFalse(SourceCatalog.inCategory("看球通", "电竞"));
        for (String category : Arrays.asList("全部", "足球", "篮球", "网球", "台球", "其他")) {
            assertFalse(SourceCatalog.inCategory(BiliEsports.NAME, category));
            assertFalse(SourceCatalog.inCategory(HuyaEsports.NAME, category));
        }
        assertTrue(SourceCatalog.inCategory(BiliEsports.NAME, "电竞"));
        assertTrue(SourceCatalog.inCategory(HuyaEsports.NAME, "电竞"));
    }

    @Test public void huyaRequiresAnActivePublicCompetitionAndReturnsNativeVideo() throws Exception {
        String html = "<script>var TT_ROOM_DATA = {\"type\":\"MATCH\",\"isOn\":true,"
            + "\"liveId\":\"101\",\"startTime\":1793000000,\"introduction\":\"S16 总决赛 GEN vs BLG\"};"
            + "const stream = {\"gameStreamInfoList\":[{\"sCdnType\":\"TX\","
            + "\"sFlvUrl\":\"https://tx.flv.huya.com/src\",\"sStreamName\":\"fixture\","
            + "\"sFlvUrlSuffix\":\"flv\",\"sFlvAntiCode\":\"sample=test&amp;edge=1\"}]};</script>";
        long now = 1_794_300_000_000L;
        Feeds.Match match = HuyaEsports.parseRoom(html, "lpl", "英雄联盟赛事", now);
        assertNotNull(match);
        assertTrue(Esports.isMatch(match));
        assertEquals("GEN", match.home);
        assertEquals("BLG", match.away);
        assertNull(HuyaEsports.parseRoom(html.replace("S16", "【预告】S16"), "lpl", "英雄联盟赛事", now));
        assertNull(HuyaEsports.parseRoom(html.replace("\"isOn\":true", "\"isOn\":true,\"isPayRoom\":1"),
            "lpl", "英雄联盟赛事", now));
        List<Feeds.Line> lines = HuyaEsports.parseLines(html, "lpl");
        assertEquals(1, lines.size());
        assertEquals("https://tx.flv.huya.com/src/fixture.flv?sample=test&edge=1", lines.get(0).url);
    }
}
