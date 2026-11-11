package cn.balltv.portal;
import org.json.*;import java.io.*;

/** Independently implemented public JRKAN player-envelope format.
 * This is its anonymous, client-visible URL envelope, not media DRM.
 * Expiry is checked; expired envelopes must be re-fetched from their parent.
 */
public final class PlayerEnvelope {
    static final String PUBLIC_TEMPLATE_KEY="ABCDEFGHIJKLMNOPQRSTUVWX";
    static int mx(int sum,int y,int z,int p,int e,int[] k){return ((z>>>5^y<<2)+(y>>>3^z<<4))^((sum^y)+(k[(p&3)^e]^z));}
    static int[] words(byte[] b){int[] out=new int[(b.length+3)/4];for(int i=0;i<b.length;i++)out[i/4]|=(b[i]&255)<<((i&3)*8);return out;}
    static String decrypt(String encoded)throws Exception{byte[] bytes=HttpData.decode64Bytes(encoded);if(bytes.length<8||bytes.length%4!=0||bytes.length>16384)throw new IOException("播放器数据长度无效");int[] v=words(bytes),key=new int[4],source=words(PUBLIC_TEMPLATE_KEY.getBytes("UTF-8"));System.arraycopy(source,0,key,0,Math.min(4,source.length));int n=v.length-1,y=v[0],sum=(6+52/v.length)*0x9e3779b9;
        while(sum!=0){int e=(sum>>>2)&3;for(int p=n;p>0;p--){int z=v[p-1];y=v[p]-=mx(sum,y,z,p,e,key);}int z=v[n];y=v[0]-=mx(sum,y,z,0,e,key);sum-=0x9e3779b9;}
        int length=v[n];if(length<bytes.length-7||length>bytes.length-4)throw new IOException("播放器格式或版本已变化");byte[] out=new byte[length];for(int i=0;i<length;i++)out[i]=(byte)(v[i/4]>>>((i&3)*8));return new String(out,"UTF-8");}
    public static String video(String encoded,long nowSeconds)throws Exception{JSONObject data=new JSONObject(decrypt(encoded));long ts=data.optLong("ts",0);if(ts<=0||nowSeconds-ts>300||ts-nowSeconds>300)throw new IOException("线路已过期，请重新连接");String url=data.optString("url");if(!StreamResolver.direct(url))throw new IOException("未返回受支持的视频地址");return url;}
}
