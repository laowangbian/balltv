package cn.balltv.portal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Recent observations from this device. A playable stream is not necessarily a match feed. */
final class LineQuality {
    static final int UNKNOWN = 0, MATCH = 1, MIXED = 2, SUSPECT = 3, FAILED = 4;
    private static final long LIFETIME = 30 * 60_000L;
    private static final Map<String, Observation> RECENT = new LinkedHashMap<>();

    private static final class Observation {
        final String event;
        final int content, height;
        final long at;

        Observation(Feeds.Match match, int content, int height, long at) {
            event = StreamHealth.key(match);
            this.content = content;
            this.height = height;
            this.at = at;
        }
    }

    static synchronized void record(Feeds.Match match, Feeds.Line line, int content, int height, long now) {
        String key = StreamHealth.lineKey(match, line);
        Observation previous = RECENT.get(key);
        if (recent(previous, now)) {
            if (content == UNKNOWN && previous.content != FAILED) return;
            if (content == MATCH && height == 0) {
                if (previous.content == FAILED || previous.content == SUSPECT || previous.content == MIXED) return;
                height = previous.height;
            }
        }
        RECENT.remove(key);
        RECENT.put(key, new Observation(match, content, height, now));
        StreamHealth.trim(RECENT, 256);
    }

    private static boolean recent(Observation observation, long now) {
        return observation != null && now >= observation.at && now - observation.at <= LIFETIME;
    }

    static boolean restrictedLabel(String name) {
        return SourceSafety.promotional(name) || name.matches(".*(动画直播|动画赛况|文字直播|比分直播|主播陪看|主播互动).*");
    }

    static synchronized int lineScore(Feeds.Match match, Feeds.Line line, long now) {
        Observation sample = RECENT.get(StreamHealth.lineKey(match, line));
        if (StreamHealth.bad(match, line, now)) return 500;
        if (restrictedLabel(line.name) || recent(sample, now) && sample.content == SUSPECT) return 350;
        if (recent(sample, now) && sample.content == MATCH)
            return -200 - Math.min(1080, Math.max(0, sample.height)) / 45;
        if (recent(sample, now) && sample.content == MIXED) return 100;
        return 0;
    }

    static synchronized int matchScore(Feeds.Match match, long now) {
        int observed = 0;
        boolean hasSample = false;
        String event = StreamHealth.key(match);
        for (Observation sample : RECENT.values()) {
            if (!recent(sample, now) || !sample.event.equals(event)) continue;
            int score = sample.content == MATCH ? -400 - Math.min(1080, Math.max(0, sample.height)) / 45
                : sample.content == SUSPECT || sample.content == FAILED ? 150 : sample.content == MIXED ? 75 : 0;
            observed = hasSample ? Math.min(observed, score) : score;
            hasSample = true;
        }
        return observed + (StreamHealth.good(match, now) == null ? 0 : -35);
    }

    static synchronized boolean hasCleanAlternative(Feeds.Match current, Feeds.Line line,
            List<Feeds.Match> members, long now) {
        String key = StreamHealth.lineKey(current, line);
        for (Feeds.Match match : members) {
            String event = StreamHealth.key(match);
            for (Map.Entry<String, Observation> entry : RECENT.entrySet()) {
                Observation sample = entry.getValue();
                if (!entry.getKey().equals(key) && sample.event.equals(event)
                        && sample.content == MATCH && recent(sample, now)) return true;
            }
        }
        return false;
    }

    static int frame(int[] pixels, String sport) {
        if (pixels.length != 48 * 27) return UNKNOWN;
        String type = Events.sport(sport);
        boolean courtSport = type.equals("football") || type.equals("basketball")
            || type.equals("tennis") || type.equals("snooker");
        if (type.equals("esports")) return UNKNOWN;
        int skin = 0, flat = 0, center = 0, bottomLeft = 0, bottomRight = 0;
        for (int y = 3; y < 24; y++) for (int x = 1; x < 48; x++) {
            int color = pixels[y * 48 + x];
            if (color == pixels[y * 48 + x - 1]) flat++;
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
            boolean skinTone = r > 95 && g > 55 && r > g * 1.10 && g > b * 1.08 && r - b < 115;
            if (y >= 13 && x >= 36 && skinTone) bottomRight++;
            if (y >= 13 && x <= 12 && skinTone) bottomLeft++;
            if (x >= 12 && x < 36 && y >= 5 && y < 23) {
                center++;
                if (skinTone) skin++;
            }
        }
        // Unknown camera angles remain eligible. Flat graphic feeds and tight presenter shots
        // require repeated observations before the player considers switching.
        if (flat > 920) return SUSPECT;
        if (courtSport && SportsPreview.acceptable(pixels, type)) {
            if (type.equals("football") && Math.max(bottomLeft, bottomRight) >= 48
                    && Math.abs(bottomLeft - bottomRight) >= 37) return MIXED;
            return MATCH;
        }
        if (skin * 100 > center * 34) return SUSPECT;
        return UNKNOWN;
    }
}
