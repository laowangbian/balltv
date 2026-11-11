package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class SportsPreviewTest {
 int[] field(boolean basketball){int[] p=new int[48*27];for(int i=0;i<p.length;i++){int v=i%48;if(basketball)p[i]=0xff000000|((100+v*3)<<16)|((70+v*2)<<8)|(30+v);else p[i]=0xff000000|((20+v)<<16)|((60+v*3)<<8)|(15+v);}return p;}
 @Test public void variedFootballPitchIsAccepted(){assertTrue(SportsPreview.acceptable(field(false),"1"));}
 @Test public void flatGreenPosterIsNotTreatedAsMatchFrame(){int[] p=new int[48*27];java.util.Arrays.fill(p,0xff208020);assertFalse(SportsPreview.acceptable(p,"football"));}
 @Test public void qrAndSkinColoredPosterIsWithheld(){int[] p=new int[48*27];for(int i=0;i<p.length;i++)p[i]=i%4==0?0xff000000:i%4==1?0xffffffff:0xffe6bba6;assertFalse(SportsPreview.acceptable(p,"football"));assertFalse(SportsPreview.acceptable(p,"basketball"));}
 @Test public void variedWoodenCourtIsAccepted(){assertTrue(SportsPreview.acceptable(field(true),"2"));}
}
