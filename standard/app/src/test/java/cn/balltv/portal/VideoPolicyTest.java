package cn.balltv.portal;
import org.junit.Test;
import static org.junit.Assert.*;

public class VideoPolicyTest {
 @Test public void healthyStandardSignalBeatsFailingHighDefinition(){assertTrue(VideoPolicy.compareLines(0,"标清",2,"1080p超清","high")<0);assertTrue(VideoPolicy.compareLines(2,"1080p",1,"标清","auto")>0);}
 @Test public void equalHealthPrefersHdAndAvoids4k(){assertTrue(VideoPolicy.compareLines(1,"1080p",1,"标清","high")<0);assertTrue(VideoPolicy.compareLines(1,"高清",1,"4K","auto")<0);assertEquals(0,VideoPolicy.compareLines(1,"线路1",1,"线路2","auto"));}
 @Test public void smoothModePrefers720Over1080(){assertTrue(VideoPolicy.compareLines(1,"720p高清",1,"1080p超清","smooth")<0);assertEquals(720,VideoPolicy.maxHeight("smooth"));}
 @Test public void unsupportedOrLowMemoryDevicesCannotEnableEnhancement(){assertFalse(VideoPolicy.enhancementEligible(19,false,512,0x30000));assertFalse(VideoPolicy.enhancementEligible(20,false,512,0x30000));assertFalse(VideoPolicy.enhancementEligible(34,true,512,0x30000));assertFalse(VideoPolicy.enhancementEligible(34,false,128,0x30000));assertFalse(VideoPolicy.enhancementEligible(34,false,512,0x20000));assertTrue(VideoPolicy.enhancementEligible(28,false,256,0x30000));}
 @Test public void androidFiveAndSixCanEnhanceWithAdequateHardware(){assertTrue(VideoPolicy.enhancementEligible(21,false,192,0x30000));assertTrue(VideoPolicy.enhancementEligible(22,false,256,0x30000));assertTrue(VideoPolicy.enhancementEligible(23,false,192,0x30000));assertFalse(VideoPolicy.enhancementEligible(21,false,256,0x20000));assertFalse(VideoPolicy.enhancementEligible(23,true,256,0x30000));}
 @Test public void targetBitrateIsBounded(){assertEquals(4000000,VideoPolicy.maxBitrate("smooth",true));assertEquals(6000000,VideoPolicy.maxBitrate("auto",true));assertEquals(12000000,VideoPolicy.maxBitrate("high",false));}
}
