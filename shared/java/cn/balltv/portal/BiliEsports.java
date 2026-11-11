package cn.balltv.portal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public Bilibili tournament room. The room's live state alone is insufficient. */
final class BiliEsports {
    static final String NAME = "B站赛事";
    static final String BASE = "https://live.bilibili.com";
    private static final String API = "https://api.live.bilibili.com/room/v1/Room/";
    private static final String ROOM = "6";
    private static final String PAGE = BASE + "/" + ROOM;
    private static final Pattern DUEL = Pattern.compile(
        "(?i)([\\p{L}\\p{N}._-]{2,24})\\s+(?:vs\\.?|对阵)\\s+([\\p{L}\\p{N}._-]{2,24})");
    private static final Pattern NON_MATCH = Pattern.compile(
        "(?i)(预告|倒计时|即将|未开赛|稍后|重播|回放|录播|集锦|赛后|切片|精彩时刻|rebroadcast|replay|highlights)");

    static Feeds.Page fetch(String view, int page) throws Exception {
        Feeds.Page result = new Feeds.Page();
        if (!view.equals("live") || page != 1) return result;
        Feeds.Match match = parseRoom(request("get_info?room_id=" + ROOM), System.currentTimeMillis());
        if (match != null) result.items.add(match);
        result.note = match == null ? "暂无确认的赛事直播" : "已连接";
        return result;
    }

    static Feeds.Match parseRoom(JSONObject data, long now) throws Exception {
        if (data.optInt("live_status") != 1) return null;
        long started = Events.epoch(data.optString("live_time"));
        return matchFromTitle(NAME, ROOM, "英雄联盟赛事", data.optString("title"),
            String.valueOf(started), started, now);
    }

    static Feeds.Match matchFromTitle(String source, String room, String league, String title,
            String liveId, long started, long now) throws Exception {
        if (liveId.isEmpty() || started <= 0 || started > now
                || NON_MATCH.matcher(title).find() || SourceSafety.promotional(title)) return null;
        Matcher duel = DUEL.matcher(title);
        if (!duel.find()) return null;
        String home = duel.group(1), away = duel.group(2);
        if (home.equalsIgnoreCase(away)) return null;
        String id = room + "-" + liveId + "-" + Integer.toHexString(title.hashCode());
        long estimate = Esports.observedStart(source + ":" + id, started, now);
        if (now - estimate > LiveState.window("esports")) return null;
        Feeds.Match match = MoreSources.match(source, id);
        match.title = title;
        match.home = home;
        match.away = away;
        match.league = league;
        match.time = String.valueOf(estimate);
        match.estimated = true;
        match.sport = "esports";
        match.status = "live";
        match.eventState = "live";
        match.room = room;
        MoreSources.pack(match);
        return match;
    }

    static ArrayList<Feeds.Line> lines(Feeds.Match match) throws Exception {
        Feeds.Match current = parseRoom(request("get_info?room_id=" + ROOM), System.currentTimeMillis());
        if (current == null || !current.id.equals(match.id)) return new ArrayList<>();
        return parseLines(request("playUrl?cid=" + ROOM + "&quality=0&platform=web"));
    }

    static ArrayList<Feeds.Line> parseLines(JSONObject data) throws Exception {
        ArrayList<Feeds.Line> result = new ArrayList<>();
        JSONArray urls = data.optJSONArray("durl");
        if (urls == null) return result;
        for (int i = 0; i < Math.min(urls.length(), 4); i++) {
            JSONObject item = urls.optJSONObject(i);
            String address = Feeds.str(item, "url");
            if (!HttpData.publicUrl(address)) continue;
            URL url = new URL(address);
            String host = url.getHost().toLowerCase(Locale.ROOT);
            String path = url.getPath().toLowerCase(Locale.ROOT);
            if (!url.getProtocol().equals("https") || !host.endsWith(".bilivideo.com")
                    || !path.endsWith(".flv") && !path.endsWith(".m3u8")) continue;
            Feeds.Line line = new Feeds.Line("B站线路 " + (i + 1), host, address);
            line.referer = PAGE;
            result.add(line);
        }
        return result;
    }

    private static JSONObject request(String path) throws Exception {
        JSONObject json = new JSONObject(HttpData.get(API + path, PAGE, false).body);
        JSONObject data = json.optJSONObject("data");
        if (json.optInt("code", -1) != 0 || data == null) throw new IOException("B站赛事接口暂不可用");
        return data;
    }
}
