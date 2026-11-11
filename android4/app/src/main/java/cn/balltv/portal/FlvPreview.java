package cn.balltv.portal;
import java.io.*;import java.util.*;import com.google.android.exoplayer2.util.NalUnitUtil;
/** Remux one AVC keyframe into MP4; no software video decoder or full FLV playback. */
final class FlvPreview {
 static final class Frame {final byte[] config,sample;Frame(byte[] c,byte[] s){config=c;sample=s;}}
 static Frame find(byte[] b){if(b.length<13||b[0]!='F'||b[1]!='L'||b[2]!='V')return null;int p=int32(b,5)+4;byte[] config=null;if(p<13)return null;
  while(p+11<=b.length){int size=((b[p+1]&255)<<16)|((b[p+2]&255)<<8)|(b[p+3]&255);if(size<0||p+11L+size>b.length)return null;int data=p+11;if((b[p]&255)==9&&size>6&&(b[data]&15)==7){int type=b[data+1]&255;if(type==0)config=Arrays.copyOfRange(b,data+5,data+size);else if(type==1&&(b[data]>>4&15)==1&&config!=null)return new Frame(config,Arrays.copyOfRange(b,data+5,data+size));}p+=11+size+4;}return null;
 }
 static int int32(byte[] b,int p){return (b[p]&255)<<24|(b[p+1]&255)<<16|(b[p+2]&255)<<8|b[p+3]&255;}
 interface Write {void run(DataOutputStream d)throws Exception;}
 static byte[] data(Write write)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);write.run(d);return out.toByteArray();}
 static byte[] box(String name,byte[]... parts)throws Exception{return data(d->{int size=8;for(byte[] p:parts)size+=p.length;d.writeInt(size);d.writeBytes(name);for(byte[] p:parts)d.write(p);});}
 static byte[] full(String name,int versionFlags,byte[]... parts)throws Exception{byte[][] all=new byte[parts.length+1][];all[0]=data(d->d.writeInt(versionFlags));System.arraycopy(parts,0,all,1,parts.length);return box(name,all);}
 static void matrix(DataOutputStream d)throws Exception{for(int n:new int[]{0x10000,0,0,0,0x10000,0,0,0,0x40000000})d.writeInt(n);}
 static byte[] mp4(byte[] flv)throws Exception{
  Frame f=find(flv);if(f==null||f.config.length<8)return null;int length=(f.config[6]&255)<<8|f.config[7]&255;if(8+length>f.config.length)return null;
  NalUnitUtil.SpsData s=NalUnitUtil.parseSpsNalUnit(f.config,8,8+length);if(s.width<=0||s.height<=0||s.width>7680||s.height>4320)return null;
  byte[] ftyp=box("ftyp",data(d->{d.writeBytes("isom");d.writeInt(512);d.writeBytes("isomiso2avc1mp41");}));byte[] moov=moov(f,s.width,s.height,0);moov=moov(f,s.width,s.height,ftyp.length+moov.length+8);return data(d->{d.write(ftyp);d.write(moovFinal(f,s.width,s.height,ftyp.length+moov(f,s.width,s.height,0).length+8));d.write(box("mdat",f.sample));});
 }
 static byte[] moovFinal(Frame f,int w,int h,int offset)throws Exception{return moov(f,w,h,offset);}
 static byte[] moov(Frame f,int w,int h,int offset)throws Exception{
  byte[] mvhd=full("mvhd",0,data(d->{d.writeLong(0);d.writeInt(1000);d.writeInt(40);d.writeInt(0x10000);d.writeShort(0x100);d.write(new byte[10]);matrix(d);d.write(new byte[24]);d.writeInt(2);}));
  byte[] tkhd=full("tkhd",7,data(d->{d.writeLong(0);d.writeInt(1);d.writeInt(0);d.writeInt(40);d.writeLong(0);d.writeShort(0);d.writeShort(0);d.writeShort(0);d.writeShort(0);matrix(d);d.writeInt(w<<16);d.writeInt(h<<16);}));
  byte[] mdhd=full("mdhd",0,data(d->{d.writeLong(0);d.writeInt(1000);d.writeInt(40);d.writeShort(0x55c4);d.writeShort(0);}));
  byte[] hdlr=full("hdlr",0,data(d->{d.writeInt(0);d.writeBytes("vide");d.write(new byte[12]);d.writeBytes("Preview\0");}));
  byte[] avc1=box("avc1",data(d->{d.write(new byte[6]);d.writeShort(1);d.write(new byte[16]);d.writeShort(w);d.writeShort(h);d.writeInt(0x480000);d.writeInt(0x480000);d.writeInt(0);d.writeShort(1);d.write(new byte[32]);d.writeShort(24);d.writeShort(-1);}),box("avcC",f.config));
  byte[] stsd=full("stsd",0,data(d->d.writeInt(1)),avc1),stts=full("stts",0,data(d->{d.writeInt(1);d.writeInt(1);d.writeInt(40);})),stsc=full("stsc",0,data(d->{d.writeInt(1);d.writeInt(1);d.writeInt(1);d.writeInt(1);})),stsz=full("stsz",0,data(d->{d.writeInt(f.sample.length);d.writeInt(1);})),stco=full("stco",0,data(d->{d.writeInt(1);d.writeInt(offset);}));
  byte[] stbl=box("stbl",stsd,stts,stsc,stsz,stco),dref=full("dref",0,data(d->d.writeInt(1)),full("url ",1));byte[] minf=box("minf",full("vmhd",1,new byte[8]),box("dinf",dref),stbl);
  return box("moov",mvhd,box("trak",tkhd,box("mdia",mdhd,hdlr,minf)));
 }
}
