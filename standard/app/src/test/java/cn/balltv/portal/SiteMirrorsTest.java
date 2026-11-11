package cn.balltv.portal;
import org.junit.*;import static org.junit.Assert.*;import java.util.*;import java.io.*;
public class SiteMirrorsTest {
 @Before public void reset(){SiteMirrors.SELECTED.clear();SiteMirrors.FAILED.clear();}
 @Test public void failedEntryMovesBehindVerifiedAlternative(){String one="https://jyu01.com/webapi/home/getHomeData";SiteMirrors.failure(one,1000);assertTrue(SiteMirrors.candidates(one,2000).get(0).startsWith("https://jytv508.com/"));SiteMirrors.success("https://jytv508.com/webapi/home/getHomeData");assertEquals("https://jytv508.com",SiteMirrors.base("鲸鱼"));}
 @Test public void onlyConfirmedServiceOriginsCanChange(){assertEquals(Collections.singletonList("https://unrelated.example/api"),SiteMirrors.candidates("https://unrelated.example/api",1000));assertEquals("https://www.88kanqiu.tw/live/1/play",SiteMirrors.referer("https://www.88kanqiu.la/live/1/play","https://www.88kanqiu.tw/live/1/source"));assertEquals("https://outside.example/",SiteMirrors.referer("https://outside.example/","https://www.88kanqiu.tw/"));}
 @Test public void writesAndLoginNeverUseReadOnlyMirrorRetry(){assertTrue(SiteMirrors.readOnly("https://jyu01.com/webapi/home/getHomeData","{}"));assertFalse(SiteMirrors.readOnly("https://jyu01.com/login","{}"));assertFalse(SiteMirrors.readOnly("https://jyu01.com/api/pay","{}"));}
 @Test public void htmlChallengeCannotBecomeSuccessfulApi()throws Exception{try{SiteMirrors.validate("https://jytv508.com/webapi/home/getHomeData","<html>challenge</html>");fail();}catch(IOException expected){}SiteMirrors.validate("https://jytv508.com/webapi/home/getHomeData","{\"status\":0,\"data\":{}}");}
}
