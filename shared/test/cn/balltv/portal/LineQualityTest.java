package cn.balltv.portal;

import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class LineQualityTest {
    @Test public void actualMatchOutranksPlayablePromotionAndUnknown() {
        long now = 100_000;
        Feeds.Match match = new Feeds.Match();
        match.source = "咖啡";
        match.id = "quality-test";
        match.title = "测试比赛";
        Feeds.Line promo = new Feeds.Line("主播互动", "a", "");
        Feeds.Line clean = new Feeds.Line("线路二", "b", "");
        Feeds.Line hd = new Feeds.Line("线路三", "c", "");
        Feeds.Line unknown = new Feeds.Line("线路四", "d", "");
        LineQuality.record(match, promo, LineQuality.SUSPECT, 1080, now);
        LineQuality.record(match, clean, LineQuality.MATCH, 540, now);
        LineQuality.record(match, hd, LineQuality.MATCH, 1080, now);
        assertTrue(LineQuality.lineScore(match, hd, now) < LineQuality.lineScore(match, clean, now));
        assertTrue(LineQuality.lineScore(match, clean, now) < LineQuality.lineScore(match, unknown, now));
        assertTrue(LineQuality.lineScore(match, unknown, now) < LineQuality.lineScore(match, promo, now));
    }

    @Test public void graphicsAreSuspiciousForCourtSportsButGameHudIsNot() {
        int[] pixels = new int[48 * 27];
        java.util.Arrays.fill(pixels, 0xff146b20);
        assertEquals(LineQuality.SUSPECT, LineQuality.frame(pixels, "football"));
        assertEquals(LineQuality.UNKNOWN, LineQuality.frame(pixels, "esports"));
    }

    @Test public void requiresCompetitionContextForGameBroadcasts() {
        assertTrue(Esports.tournament("英雄联盟全球总决赛 BLG vs GEN"));
        assertTrue(Esports.tournament("CS2 IEM 淘汰赛"));
        assertFalse(Esports.tournament("主播 LOL 冲分"));
        assertFalse(Esports.tournament("王者荣耀决赛解说与排位教学"));
    }

    @Test public void officialRoomCanBeOnAirWithOnlyAnAnnouncement() throws Exception {
        long now = 1_799_000_000_000L;
        JSONObject room = new JSONObject().put("live_status", 1).put("live_time", now - 60_000L)
            .put("title", "【预告】15号3点 GEN vs BLG");
        assertNull(BiliEsports.parseRoom(room, now));
        room.put("title", "S16 全球总决赛 GEN vs BLG");
        Feeds.Match match = BiliEsports.parseRoom(room, now);
        assertNotNull(match);
        assertEquals("GEN", match.home);
        assertEquals("BLG", match.away);
        assertTrue(Esports.tournament(match));
        assertEquals(LiveState.Phase.LIVE, LiveState.phase(match, java.util.Collections.singletonList(match), now));
        room.put("title", "重播 GEN vs BLG");
        assertNull(BiliEsports.parseRoom(room, now));
    }
}
