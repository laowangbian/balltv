package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class SourceSafetyTest {
 @Test public void promotionalRoomIsNotASearchSuggestion(){assertTrue(SourceSafety.promotional("私信 +777 领取视频福利"));assertTrue(SourceSafety.promotional("下载 APP 领取彩金"));assertFalse(SourceSafety.promotional("NBA 回放"));assertFalse(SourceSafety.promotional("中女甲"));}
 @Test public void preservesKnownMatchButCleansPromotionCategory(){Feeds.Match m=new Feeds.Match();m.home="甲队";m.away="乙队";m.title="比赛";m.league="私信领取福利";assertTrue(SourceSafety.usable(m));assertEquals("体育赛事",m.league);m.home="";m.away="";m.title="私信领取福利";assertFalse(SourceSafety.usable(m));}
}
