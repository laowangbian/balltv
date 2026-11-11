package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class SearchSessionTest {
 @Test public void repeatedSearchesReturnToOriginalFiltersFocusAndScroll(){SearchSession s=new SearchSession();SearchSession.Snapshot before=new SearchSession.Snapshot("So米","篮球","NBA","2026-11-09",420,"event:123");s.begin("NBA",before);s.begin("湖人",new SearchSession.Snapshot("鲸鱼","全部","全部联赛","",0,"search"));assertSame(before,s.clear());assertNull(s.origin());assertEquals(420,before.scroll);assertEquals("event:123",before.focus);}
 @Test public void emptySearchDoesNotReplaceOriginAndRestoredStateSurvivesRecreation(){SearchSession s=new SearchSession();s.begin(" ",new SearchSession.Snapshot("a","b","c","d",0,"f"));assertNull(s.origin());SearchSession.Snapshot before=new SearchSession.Snapshot("a","b","c","d",30,"f");s.restore(before);assertSame(before,s.clear());}
 @Test public void backClearsSearchBeforeFiltersAndNeverExitsSearch(){assertEquals(SearchSession.Back.SEARCH,SearchSession.back("NBA",true,"schedule",true));assertEquals(SearchSession.Back.FILTERS,SearchSession.back("",true,"live",false));assertEquals(SearchSession.Back.HOME,SearchSession.back("",false,"schedule",false));assertEquals(SearchSession.Back.ARM_EXIT,SearchSession.back("",false,"live",false));assertEquals(SearchSession.Back.EXIT,SearchSession.back("",false,"live",true));}
}
