package cn.balltv.portal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Uses the public, signed-out player data on the platform's esports page. */
final class KuaishouEsports {
    static final String NAME = "快手";
    static final String BASE = "https://live.kuaishou.com";
    private static final String PAGE = BASE + "/match";
    private static final String STATE = "window.__INITIAL_STATE__=";
    private static final Pattern NON_LIVE = Pattern.compile("预告|回放|重播|录播|集锦|排位|教学|选秀|抽签");
    private static volatile Catalog cached;

    static String eventIdentity(Feeds.Match match) {
        if (!NAME.equals(match.source) || !Esports.tournament(match)) return "";
        return "esports|kuaishou|" + match.league + "|" + match.title;
    }

    static Feeds.Page fetch(String view, int page) throws Exception {
        Feeds.Page result = new Feeds.Page();
        if (!view.equals("live") || page != 1) return result;
        JSONArray rows = catalog().rows;
        long now = System.currentTimeMillis();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            Feeds.Match match = match(row, now);
            if (match != null) result.items.add(match);
        }
        result.note = result.items.isEmpty() ? "暂无确认的赛事直播" : "已连接";
        return result;
    }

    static Feeds.Match match(JSONObject row, long now) throws Exception {
        String title = row.optString("caption").trim();
        String game = Feeds.str(row.optJSONObject("gameInfo"), "name");
        String id = row.optString("id");
        long started = row.optLong("statrtTime");
        if (!row.optString("type").equals("live") || row.optBoolean("privateLive")
                || !id.matches("[A-Za-z0-9_-]{5,64}") || started <= 0 || started > now
                || now - started > LiveState.window("esports") || NON_LIVE.matcher(title).find()
                || !Esports.tournament(game + " " + title) || parseLines(row).isEmpty()) return null;

        String key = id + "-" + Integer.toHexString(title.hashCode());
        Feeds.Match match = BiliEsports.matchFromTitle(NAME, id, game, title, id, started, now);
        if (match == null) {
            match = MoreSources.match(NAME, key);
            match.title = title;
            match.league = game;
            match.time = String.valueOf(started);
            match.estimated = true;
            match.sport = "esports";
            match.status = "live";
            match.eventState = "live";
            match.room = id;
        }
        String cover = row.optString("poster");
        if (HttpData.publicUrl(cover)) match.cover = cover;
        MoreSources.pack(match);
        return match;
    }

    static ArrayList<Feeds.Line> lines(Feeds.Match requested) throws Exception {
        JSONArray rows = catalog().rows;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null || !requested.room.equals(row.optString("id"))) continue;
            Feeds.Match current = match(row, System.currentTimeMillis());
            if (current != null && requested.id.equals(current.id)) return parseLines(row);
        }
        return new ArrayList<>();
    }

    private static ArrayList<Feeds.Line> parseLines(JSONObject row) throws Exception {
        ArrayList<Feeds.Line> lines = new ArrayList<>();
        JSONArray manifests = row.optJSONArray("playUrls");
        if (manifests == null) return lines;
        for (int i = 0; i < manifests.length() && lines.size() < 4; i++) {
            JSONObject manifest = manifests.optJSONObject(i);
            JSONObject set = manifest == null ? null : manifest.optJSONObject("adaptationSet");
            JSONArray renditions = set == null ? null : set.optJSONArray("representation");
            if (renditions == null) continue;
            for (int j = 0; j < renditions.length(); j++) {
                JSONObject rendition = renditions.optJSONObject(j);
                if (rendition == null || rendition.optBoolean("hidden")
                        || !rendition.optString("qualityType").equals("STANDARD")) continue;
                String address = rendition.optString("url");
                if (!HttpData.publicUrl(address)) continue;
                URL url = new URL(address);
                if (!url.getProtocol().equals("https") || !url.getHost().endsWith(".pull.yximgs.com")
                        || !url.getPath().endsWith(".flv")) continue;
                Feeds.Line line = new Feeds.Line("快手 · 高清", url.getHost() + ":" + row.optString("id"), address);
                line.referer = PAGE;
                lines.add(line);
                break;
            }
        }
        return lines;
    }

    private static Catalog catalog() throws Exception {
        Catalog current = cached;
        long now = android.os.SystemClock.elapsedRealtime();
        if (current != null && now - current.at < 20_000) return current;
        String html = HttpData.get(PAGE, BASE, false).body;
        int start = html.indexOf(STATE);
        if (start < 0) throw new IOException("快手赛事页缺少播放器数据");
        Object value = new JSONTokener(html.substring(start + STATE.length())).nextValue();
        JSONObject card = value instanceof JSONObject ? ((JSONObject) value).optJSONObject("matchCard") : null;
        JSONArray rows = card == null ? null : card.optJSONArray("list");
        if (rows == null) throw new IOException("快手赛事目录格式已变更");
        current = new Catalog(rows, android.os.SystemClock.elapsedRealtime());
        cached = current;
        return current;
    }

    private static final class Catalog {
        final JSONArray rows;
        final long at;
        Catalog(JSONArray rows, long at) {
            this.rows = rows;
            this.at = at;
        }
    }
}
