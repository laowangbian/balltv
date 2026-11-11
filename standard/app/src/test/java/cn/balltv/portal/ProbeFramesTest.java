package cn.balltv.portal;
import org.junit.*;import static org.junit.Assert.*;
public class ProbeFramesTest {
 @Before public void reset(){ProbeFrames.clear();}
 @Test public void compressedEvidenceIsConsumedOnceAndExpires(){byte[] sample=new byte[100];ProbeFrames.put("one",sample,1000);assertSame(sample,ProbeFrames.take("one",2000));assertNull(ProbeFrames.take("one",2001));ProbeFrames.put("expired",sample,1000);assertNull(ProbeFrames.take("expired",31000));}
 @Test public void oldEvidenceIsEvictedBeforeMemoryCanGrowUnbounded(){for(int i=0;i<10;i++)ProbeFrames.put("key"+i,new byte[524288],1000);assertTrue(ProbeFrames.bytes<=ProbeFrames.MAX_BYTES);assertNull(ProbeFrames.take("key0",1001));assertNotNull(ProbeFrames.take("key9",1001));}
}
