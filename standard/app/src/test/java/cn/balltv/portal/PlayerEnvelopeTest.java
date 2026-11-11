package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class PlayerEnvelopeTest {
    // Test vector computed by the site's public client using a fictitious media URL.
    final String value="Zh2AN/xTNdXZpKRW9t7yKKXrDty2OK3JGn3pVqRa69YwzuUK2e7bvZrDvT1pgZJh+zGy4pjsMgVHr87VAjd8GA==";
    @Test public void decodesAnonymousPlayerFormat()throws Exception{assertEquals("https://media.example/live.m3u8",PlayerEnvelope.video(value,1794190000));}
    @Test public void expiredEnvelopeIsRejectedRatherThanBypassed()throws Exception{try{PlayerEnvelope.video(value,1794190301);fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("过期"));}}
    @Test public void invalidCiphertextDoesNotBecomeAUrl()throws Exception{try{PlayerEnvelope.video("AAAA",1794190000);fail();}catch(java.io.IOException expected){}}
}
