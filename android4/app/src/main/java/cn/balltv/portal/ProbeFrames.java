package cn.balltv.portal;
import java.util.*;
/** Short-lived compressed evidence already downloaded by the live checker. */
final class ProbeFrames {
 static final int MAX_BYTES=2*1024*1024;static int bytes;static int limit=MAX_BYTES;
 static final class Sample {final byte[] data;final long at;Sample(byte[] d,long at){data=d;this.at=at;}}
 static final LinkedHashMap<String,Sample> DATA=new LinkedHashMap<String,Sample>();
 static synchronized void put(String key,byte[] data,long now){if(data==null||data.length==0||data.length>524288)return;Sample old=DATA.remove(key);if(old!=null)bytes-=old.data.length;DATA.put(key,new Sample(data,now));bytes+=data.length;trim();}
 static synchronized void setLimit(int maximum){limit=maximum;trim();}
 static void trim(){while(bytes>limit){String first=DATA.keySet().iterator().next();bytes-=DATA.remove(first).data.length;}}
 static synchronized byte[] take(String key,long now){Sample sample=DATA.remove(key);if(sample==null)return null;bytes-=sample.data.length;return now>=sample.at&&now-sample.at<30000?sample.data:null;}
 static synchronized void clear(){DATA.clear();bytes=0;}
}
