package cn.balltv.portal;

import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;

final class Esports {
    private static final Map<String, Long> OBSERVED_STARTS = new LinkedHashMap<>();
    private static final Pattern GAME = Pattern.compile(
        "(?i)(英雄联盟|王者荣耀|无畏契约|反恐精英|第五人格|和平精英|绝地求生|永劫无间|穿越火线|CF手游|QQ飞车|云顶之弈|火影忍者|"
        + "\\b(?:lol|cs2|csgo|dota\\s*2?|valorant|pubg|lpl|lck|lec|lcs|kpl|vct|ivl|pel|iem)\\b)");
    private static final Pattern MATCH = Pattern.compile(
        "(?i)(职业联赛|冠军赛|锦标赛|全球总决赛|世界赛|杯赛|季后赛|淘汰赛|小组赛|资格赛|预选赛|瑞士轮|"
        + "常规赛|半决赛|决赛|赛事直播|\\bvs\\.?\\b|对阵)");
    private static final Pattern PERSONAL = Pattern.compile(
        "(排位|冲分|上分|教学|陪玩|娱乐局|水友赛|直播带货|直播回放|录播)");

    static boolean tournament(String text) {
        return GAME.matcher(text).find() && MATCH.matcher(text).find() && !PERSONAL.matcher(text).find();
    }

    static boolean tournament(Feeds.Match match) {
        String text = match.league + " " + match.title;
        if (PERSONAL.matcher(text).find()) return false;
        if (tournament(text)) return true;
        return Events.sport(match.sport).equals("esports")
            && !match.home.isEmpty() && !match.away.isEmpty() && !match.home.equals(match.away)
            && GAME.matcher(text).find();
    }

    static boolean isMatch(Feeds.Match match) {
        return Events.sport(match.sport).equals("esports") || tournament(match);
    }

    static synchronized long observedStart(String event, long roomStarted, long now) {
        Long previous = OBSERVED_STARTS.get(event);
        if (previous != null) return previous;
        long estimated = now - roomStarted > LiveState.window("esports") ? now : roomStarted;
        OBSERVED_STARTS.put(event, estimated);
        StreamHealth.trim(OBSERVED_STARTS, 32);
        return estimated;
    }
}
