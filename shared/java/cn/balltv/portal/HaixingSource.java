package cn.balltv.portal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

final class HaixingSource {
    static final String NAME = "海星体育";
    static final String BASE = "https://hxty4.com";
    private static final Pattern FIXTURE = Pattern.compile(
        "^(?:【[^】]{1,8}】\\s*)?(?:\\d{1,2}:\\d{2}\\s+)?"
        + "(国际友谊(?:赛|U\\d+)?|友谊赛|熊猫杯|日联杯|欧冠|英超|西甲|意甲|德甲|法甲|中超|NBA|CBA|WNBA|NCAA)"
        + "\\s+(?:\\d{1,2}:\\d{2}\\s+)?(.{1,28}?)\\s*(?:vs\\.?|对阵|\\s-\\s)\\s*(.{1,28})$",
        Pattern.CASE_INSENSITIVE);

    static Feeds.Page fetch(String view, int page) throws Exception {
        Feeds.Page result = new Feeds.Page();
        if (!view.equals("live") || page != 1) return result;
        JSONObject props = pageProps(BASE + "/live");
        JSONObject anchors = props.optJSONObject("liveAnchorList");
        JSONArray rows = anchors == null ? null : anchors.optJSONArray("records");
        if (rows == null) throw new IOException("海星赛事目录格式已变更");
        long now = System.currentTimeMillis();
        for (int i = 0; i < rows.length() && result.items.size() < 16; i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null || row.optInt("status") != 1 || row.optInt("isHide") != 0) continue;
            String id = Feeds.str(row, "matchId");
            String room = Feeds.str(row, "roomId");
            if (!id.matches("[1-9][0-9]{0,15}") || !room.matches("[1-9][0-9]{0,10}")) continue;
            String title = Feeds.str(row, "title").trim();
            if (SourceSafety.promotional(title) || title.matches(".*(回放|录播|重播|集锦|预告).*")) continue;
            Matcher fixture = FIXTURE.matcher(title);
            if (!fixture.matches()) continue;
            String sport = Feeds.str(row, "liveTypeName");
            if (!sport.equals("足球") && !sport.equals("篮球")) continue;
            String league = fixture.group(1);
            boolean basketball = league.matches("(?i)NBA|CBA|WNBA|NCAA");
            if (basketball != sport.equals("篮球")) continue;
            Feeds.Match match = MoreSources.match(NAME, id + "-" + room);
            match.title = title;
            match.league = league;
            match.home = fixture.group(2).trim();
            match.away = fixture.group(3).trim();
            match.sport = basketball ? "basketball" : "football";
            match.room = room;
            match.time = String.valueOf(Esports.observedStart(NAME + ":" + match.id, now, now));
            match.estimated = true;
            match.status = "live";
            match.eventState = "live";
            match.raw.put("matchId", id);
            MoreSources.pack(match);
            result.items.add(match);
        }
        result.note = result.items.isEmpty() ? "暂无确认的赛事直播" : "已连接";
        return result;
    }

    static ArrayList<Feeds.Line> lines(Feeds.Match match) throws Exception {
        ArrayList<Feeds.Line> result = new ArrayList<>();
        if (!match.room.matches("[1-9][0-9]{0,10}")) return result;
        String page = BASE + "/live/" + match.room;
        JSONObject props = pageProps(page);
        JSONObject detail = props.optJSONObject("liveDetailProp");
        String id = Feeds.str(match.raw.optJSONObject("extra"), "matchId");
        if (id.isEmpty() || !props.optBoolean("canPlay") || props.optBoolean("playBack")
                || detail == null || detail.optInt("status") != 1
                || !id.equals(Feeds.str(detail, "matchId"))) return result;
        JSONObject urls = detail.optJSONObject("playAddr");
        if (urls == null) return result;
        for (String key : new String[]{"sd_flv", "ori_flv", "ld_flv"}) {
            String address = urls.optString(key);
            if (!HttpData.publicUrl(address)) continue;
            URL url = new URL(address);
            if (!url.getProtocol().equals("https") || !url.getHost().equals("pull.xinhongtang.com")
                    || !url.getPath().endsWith(".flv")) continue;
            String label = key.startsWith("ori") ? "原画" : key.startsWith("ld") ? "流畅" : "标清";
            Feeds.Line line = new Feeds.Line(label, match.room + ":" + key, address);
            line.referer = page;
            result.add(line);
        }
        return result;
    }

    private static JSONObject pageProps(String page) throws Exception {
        Element state = Jsoup.parse(HttpData.get(page, BASE, false).body).getElementById("__NEXT_DATA__");
        if (state == null) throw new IOException("海星页面未提供赛事数据");
        return new JSONObject(state.data()).getJSONObject("props").getJSONObject("pageProps");
    }
}
