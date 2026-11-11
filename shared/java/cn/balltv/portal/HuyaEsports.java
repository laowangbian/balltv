package cn.balltv.portal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Reads only the public player data published by fixed Huya tournament rooms. */
final class HuyaEsports {
    static final String NAME = "虎牙";
    static final String BASE = "https://www.huya.com";
    private static final String[] ROOMS = {"lpl", "kpl", "csgo"};
    private static final String[] LEAGUES = {"英雄联盟赛事", "王者荣耀赛事", "CS2 赛事"};

    static Feeds.Page fetch(String view, int page) throws Exception {
        Feeds.Page result = new Feeds.Page();
        if (!view.equals("live") || page != 1) return result;
        Exception last = null;
        int available = 0;
        for (int i = 0; i < ROOMS.length; i++) {
            try {
                String html = fetchRoom(ROOMS[i]);
                Feeds.Match match = parseRoom(html, ROOMS[i], LEAGUES[i], System.currentTimeMillis());
                if (match != null) result.items.add(match);
                available++;
            } catch (Exception error) {
                last = error;
            }
        }
        if (available == 0 && last != null) throw last;
        result.note = result.items.isEmpty() ? "暂无确认的赛事直播" : "已连接";
        return result;
    }

    static Feeds.Match parseRoom(String html, String room, String league, long now) throws Exception {
        JSONObject data = (JSONObject) readValue(html, "TT_ROOM_DATA", '=');
        if (!data.optString("type").equals("MATCH") || !data.optBoolean("isOn")
                || data.optBoolean("isReplay") || data.optInt("isSecret") != 0
                || data.optInt("isPayRoom") != 0 || !data.optString("roomPayPassword").isEmpty()) return null;
        String title = data.optString("introduction");
        String liveId = data.optString("liveId");
        return BiliEsports.matchFromTitle(NAME, room, league, title, liveId, data.optLong("startTime") * 1000L, now);
    }

    static ArrayList<Feeds.Line> lines(Feeds.Match match) throws Exception {
        for (int i = 0; i < ROOMS.length; i++) {
            if (!ROOMS[i].equals(match.room)) continue;
            String html = fetchRoom(match.room);
            Feeds.Match current = parseRoom(html, ROOMS[i], LEAGUES[i], System.currentTimeMillis());
            if (current == null || !current.id.equals(match.id)) return new ArrayList<>();
            return parseLines(html, match.room);
        }
        return new ArrayList<>();
    }

    static ArrayList<Feeds.Line> parseLines(String html, String room) throws Exception {
        JSONArray streams = (JSONArray) readValue(html, "\"gameStreamInfoList\"", ':');
        ArrayList<Feeds.Line> result = new ArrayList<>();
        for (int i = 0; i < streams.length() && result.size() < 8; i++) {
            JSONObject stream = streams.optJSONObject(i);
            if (stream == null) continue;
            String base = stream.optString("sFlvUrl");
            String name = stream.optString("sStreamName");
            String query = stream.optString("sFlvAntiCode").replace("&amp;", "&");
            String cdn = stream.optString("sCdnType");
            if (!name.matches("[A-Za-z0-9_-]{1,180}") || !cdn.matches("[A-Za-z0-9]{1,12}")
                    || !stream.optString("sFlvUrlSuffix").equals("flv") || query.isEmpty()
                    || !HttpData.publicUrl(base)) continue;
            URL endpoint = new URL(base);
            if (!endpoint.getHost().toLowerCase(Locale.ROOT).endsWith(".huya.com")
                    || endpoint.getQuery() != null || endpoint.getRef() != null) continue;
            Feeds.Line line = new Feeds.Line("虎牙 " + cdn + " 线路", cdn + ":" + name,
                base.replaceFirst("/$", "") + "/" + name + ".flv?" + query);
            line.referer = BASE + "/" + room;
            result.add(line);
        }
        // The TX edge was the only one that served media during the current check.
        Collections.sort(result, (a, b) -> Boolean.compare(!a.id.startsWith("TX:"), !b.id.startsWith("TX:")));
        return result;
    }

    private static Object readValue(String html, String field, char separator) throws Exception {
        int at = html.indexOf(field);
        if (at < 0) throw new IOException("虎牙赛事房间未提供播放器数据");
        int value = at + field.length();
        while (value < html.length() && Character.isWhitespace(html.charAt(value))) value++;
        if (value >= html.length() || html.charAt(value) != separator) throw new IOException("虎牙赛事数据格式已变更");
        return new JSONTokener(html.substring(value + 1)).nextValue();
    }

    private static String fetchRoom(String room) throws Exception {
        String url = BASE + "/" + room;
        return HttpData.get(url, url, true).body;
    }
}
