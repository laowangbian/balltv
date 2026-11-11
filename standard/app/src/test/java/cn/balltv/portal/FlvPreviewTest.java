package cn.balltv.portal;
import java.io.*;import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class FlvPreviewTest {
 static void ue(StringBuilder b,int n){String v=Integer.toBinaryString(n+1);for(int i=1;i<v.length();i++)b.append('0');b.append(v);}
 byte[] sps(){StringBuilder b=new StringBuilder();ue(b,0);ue(b,0);ue(b,0);ue(b,0);ue(b,1);b.append('0');ue(b,79);ue(b,44);b.append("11001");while(b.length()%8!=0)b.append('0');byte[] result=new byte[4+b.length()/8];result[0]=0x67;result[1]=66;result[2]=0;result[3]=31;for(int i=0;i<b.length();i+=8)result[4+i/8]=(byte)Integer.parseInt(b.substring(i,i+8),2);return result;}
 byte[] tag(byte[] payload)throws Exception{return FlvPreview.data(d->{d.writeByte(9);d.writeByte(payload.length>>16);d.writeByte(payload.length>>8);d.writeByte(payload.length);d.write(new byte[7]);d.write(payload);d.writeInt(payload.length+11);});}
 byte[] input(boolean key)throws Exception{byte[] sps=sps();byte[] config=FlvPreview.data(d->{d.write(new byte[]{1,66,0,31,(byte)255,(byte)225});d.writeShort(sps.length);d.write(sps);d.writeByte(1);d.writeShort(4);d.write(new byte[]{0x68,(byte)0xce,6,(byte)0xe2});});return FlvPreview.data(d->{d.write(new byte[]{'F','L','V',1,1,0,0,0,9,0,0,0,0});d.write(tag(FlvPreview.data(x->{x.write(new byte[]{0x17,0,0,0,0});x.write(config);})));d.write(tag(new byte[]{(byte)(key?0x17:0x27),1,0,0,0,0,0,0,3,0x65,(byte)0x88,(byte)0x84}));});}
 int type(byte[] b,String name)throws Exception{byte[] n=name.getBytes("US-ASCII");outer:for(int i=4;i<b.length-n.length;i++){for(int k=0;k<n.length;k++)if(b[i+k]!=n[k])continue outer;return i;}return -1;}
 @Test public void keyframeIsWrappedWithAvcConfigurationAndCorrectSampleOffset()throws Exception{byte[] mp4=FlvPreview.mp4(input(true));assertNotNull(mp4);assertEquals(4,type(mp4,"ftyp"));assertTrue(type(mp4,"avcC")>0);assertTrue(type(mp4,"moov")>0);assertEquals(type(mp4,"mdat")+4,FlvPreview.int32(mp4,type(mp4,"stco")+12));}
 @Test public void interFramesAndIncompleteFlvAreNotUsedAsThumbnails()throws Exception{assertNull(FlvPreview.find(input(false)));assertNull(FlvPreview.find(new byte[]{'F','L','V'}));}
}
