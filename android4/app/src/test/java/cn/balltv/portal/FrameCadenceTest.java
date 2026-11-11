package cn.balltv.portal;
import org.junit.*;import static org.junit.Assert.*;
public class FrameCadenceTest {
 @Test public void actual25And50FpsUsePresentationIntervals(){for(int fps:new int[]{25,30,50,60}){FrameCadence c=new FrameCadence();for(int n=0;n<40;n++)c.observe(Math.round(n*1000000./fps));assertEquals(fps,c.fps(),.01);}}
 @Test public void startupInsufficientSamplesDoesNotInventFrameRate(){FrameCadence c=new FrameCadence();c.observe(0);c.observe(40000);assertEquals(0,c.fps(),0);}
 @Test public void isolatedDuplicateTimestampAndGapDoNotDoubleTheReportedRate(){FrameCadence c=new FrameCadence();for(int n=0;n<12;n++)c.observe(n*40000L);c.observe(440000);c.observe(520000);assertEquals(25,c.fps(),.01);c.observe(2000000);assertEquals(0,c.fps(),0);}
}
