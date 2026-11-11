package cn.balltv.portal;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;import java.util.zip.InflaterInputStream;
import org.json.*;

/** Bounded public HTTP requests. No login, browser profile or executable page code. */
public final class HttpData {
    public static final class Reply {public final String url,body;Reply(String u,String b){url=u;body=b;}}
    private static final class Cached {long at;Reply reply;Cached(Reply r){at=System.currentTimeMillis();reply=r;}}
    private static final LinkedHashMap<String,Cached> CACHE=new LinkedHashMap<String,Cached>();
    public static boolean publicUrl(String value){try{URL u=new URL(value);String h=u.getHost().toLowerCase(Locale.ROOT);if(!u.getProtocol().equals("http")&&!u.getProtocol().equals("https"))return false;if(u.getUserInfo()!=null||h.equals("localhost")||h.endsWith(".localhost")||h.equals("::1")||h.startsWith("["))return false;if(h.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")){String[] p=h.split("\\.");int a=Integer.parseInt(p[0]),b=Integer.parseInt(p[1]);if(a==0||a==10||a==127||a>=224||a==169&&b==254||a==172&&b>=16&&b<=31||a==192&&b==168)return false;}return true;}catch(Exception e){return false;}}
    public static Reply get(String url,String referer,boolean cache)throws Exception{return request(url,referer,null,cache);}
    public static Reply get(String url,String referer,boolean cache,Map<String,String> headers)throws Exception{return request(url,referer,null,cache,-1,0,headers);}
    public static Reply post(String url,String referer,JSONObject body)throws Exception{return request(url,referer,body.toString(),false);}
    public static Reply post(String url,String referer,JSONObject body,Map<String,String> headers)throws Exception{return request(url,referer,body.toString(),false,-1,0,headers);}
    public static Reply postForm(String url,String referer,String body,Map<String,String> headers)throws Exception{Map<String,String> formHeaders=new LinkedHashMap<String,String>(headers);formHeaders.put("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");return request(url,referer,body,false,-1,0,formHeaders);}
    private static Reply request(String url,String referer,String data,boolean cache)throws Exception{
        return request(url,referer,data,cache,-1,0);
    }
    public static Reply arrayPage(String url,String referer,int start,int count)throws Exception{return request(url,referer,null,true,Math.max(0,start),count);}
    private static Reply request(String url,String referer,String data,boolean cache,int start,int count)throws Exception{
        return request(url,referer,data,cache,start,count,Collections.<String,String>emptyMap());
    }
    private static Reply request(String url,String referer,String data,boolean cache,int start,int count,Map<String,String> headers)throws Exception{
        long deadline=RequestBudget.deadline(System.nanoTime()+28000000000L);List<String> targets=SiteMirrors.readOnly(url,data)?SiteMirrors.candidates(url,System.currentTimeMillis()):Collections.singletonList(url);Exception last=null;for(String target:targets){if(System.nanoTime()>deadline)break;try{Reply reply=requestOnce(target,SiteMirrors.referer(referer,target),data,cache,start,count,headers,deadline);if(!SiteMirrors.family(target).isEmpty())SiteMirrors.validate(target,reply.body);SiteMirrors.success(target);return reply;}catch(Exception failed){last=failed;SiteMirrors.failure(target,System.currentTimeMillis());}}if(last!=null)throw last;throw new SocketTimeoutException("来源备用入口连接超时");
    }
    private static Reply requestOnce(String url,String referer,String data,boolean cache,int start,int count,Map<String,String> headers,long deadline)throws Exception{
        final String key=url+(start<0?"":"#array="+start+":"+count)+headers.toString();
        if(cache){synchronized(CACHE){Cached c=CACHE.get(key);if(c!=null&&System.currentTimeMillis()-c.at<20000)return c.reply;}}
        String originalUrl=url;boolean retried=false;
        for(int redirect=0;redirect<6;redirect++){
            if(!publicUrl(url))throw new IOException("来源返回了不支持的地址");int remaining=(int)((deadline-System.nanoTime())/1000000L);if(remaining<=0)throw new SocketTimeoutException("请求超时");
            HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(Math.min(8000,remaining));c.setReadTimeout(Math.min(6000,remaining));if(retried)c.setRequestProperty("Connection","close");c.setRequestProperty("User-Agent",Feeds.UA);c.setRequestProperty("Accept","application/json,text/html,*/*");c.setRequestProperty("Accept-Encoding","gzip,deflate");c.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.6");if(publicUrl(referer)){c.setRequestProperty("Referer",referer);URL origin=new URL(referer);c.setRequestProperty("Origin",origin.getProtocol()+"://"+origin.getAuthority());}
            for(Map.Entry<String,String> header:headers.entrySet())c.setRequestProperty(header.getKey(),header.getValue());
            try{if(data!=null){c.setRequestMethod("POST");if(!headers.containsKey("Content-Type"))c.setRequestProperty("Content-Type","application/json");c.setDoOutput(true);byte[] bytes=data.getBytes("UTF-8");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int status=c.getResponseCode();if(status>=300&&status<400){String location=c.getHeaderField("Location");if(location==null)throw new IOException("重定向地址为空");url=Feeds.absolute(url,location);if(status==303){data=null;}continue;}if(status<200||status>=300)throw new IOException("HTTP "+status);
                InputStream raw=decoded(c.getInputStream(),c.getContentEncoding());
                if(start>=0){try(Reader reader=new BufferedReader(new InputStreamReader(raw,"UTF-8"))){return new Reply(url,readArrayPage(reader,start,count,deadline).toString());}}
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=raw){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("来源响应过大");if(System.nanoTime()>deadline)throw new SocketTimeoutException("读取超时");out.write(bytes,0,n);}}
                String charset="UTF-8",ct=c.getContentType();if(ct!=null){Matcher m=Pattern.compile("charset=([^; ]+)",Pattern.CASE_INSENSITIVE).matcher(ct);if(m.find())charset=m.group(1).replace("\"","");}byte[] body=out.toByteArray();String prefix=new String(body,0,Math.min(body.length,2048),"ISO-8859-1");if(prefix.toLowerCase(Locale.ROOT).contains("charset=gb2312")||prefix.toLowerCase(Locale.ROOT).contains("charset=gbk"))charset="GB18030";
                Reply reply=new Reply(url,new String(body,charset));if(cache&&reply.body.length()<600000){synchronized(CACHE){CACHE.put(key,new Cached(reply));int size=0;for(Cached v:CACHE.values())size+=v.reply.body.length();Iterator<Map.Entry<String,Cached>> iterator=CACHE.entrySet().iterator();while((CACHE.size()>12||size>2*1024*1024)&&iterator.hasNext()){Map.Entry<String,Cached> entry=iterator.next();size-=entry.getValue().reply.body.length();iterator.remove();}}}return reply;
            }catch(EOFException|SocketException|SocketTimeoutException transientFailure){if(data!=null||retried||System.nanoTime()>deadline)throw transientFailure;retried=true;url=originalUrl;}finally{c.disconnect();}
        }throw new IOException("来源跳转过多");
    }
    static InputStream decoded(InputStream stream,String encoding)throws IOException{PushbackInputStream in=new PushbackInputStream(stream,2);int a=in.read(),b=in.read();if(b>=0)in.unread(b);if(a>=0)in.unread(a);if(a==31&&b==139)return new GZIPInputStream(in);if("deflate".equalsIgnoreCase(encoding)){boolean zlib=a>=0&&b>=0&&(a&15)==8&&((a<<8)+b)%31==0;return new InflaterInputStream(in,new java.util.zip.Inflater(!zlib));}if(encoding!=null&&!encoding.isEmpty()&&!encoding.equalsIgnoreCase("identity")&&!encoding.equalsIgnoreCase("gzip"))throw new IOException("来源返回不支持的压缩格式");return in;}
    // The upstream ignores pagination and returns thousands of videos. Keep only
    // the requested objects and close the response as soon as the page is full.
    static JSONArray readArrayPage(Reader reader,int start,int count,long deadline)throws Exception{
        if(count<1||count>100)throw new IOException("分页大小无效");JSONArray result=new JSONArray();StringBuilder item=new StringBuilder();boolean opened=false,inString=false,escaped=false,expectValue=true;int depth=0,index=0,read=0,ch;
        while((ch=reader.read())!=-1){if(++read>4*1024*1024)throw new IOException("分页读取范围过大");if(System.nanoTime()>deadline)throw new SocketTimeoutException("读取超时");char c=(char)ch;
            if(!opened){if(Character.isWhitespace(c)||c=='\ufeff')continue;if(c!='[')throw new IOException("来源未返回数组");opened=true;continue;}
            if(depth==0){if(Character.isWhitespace(c))continue;if(c==']'){if(expectValue&&index>0)throw new IOException("数组尾部分隔符无效");return result;}if(!expectValue){if(c!=',')throw new IOException("数组分隔符无效");expectValue=true;continue;}if(c!='{')throw new IOException("回放条目不是对象");depth=1;item.setLength(0);item.append(c);continue;}
            item.append(c);if(item.length()>200000)throw new IOException("回放条目过大");if(inString){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')inString=false;}else if(c=='\"')inString=true;else if(c=='{'||c=='[')depth++;else if(c=='}'||c==']')depth--;
            if(depth==0){if(index++>=start)result.put(new JSONObject(item.toString()));expectValue=false;if(result.length()==count)return result;}
        }throw new IOException("回放列表不完整");
    }
    public static Object json(String value)throws Exception {String s=value.trim();if(!s.startsWith("{")&&!s.startsWith("[")){Matcher m=Pattern.compile("^[A-Za-z_$][\\w$]*\\s*\\((.*)\\)\\s*;?$",Pattern.DOTALL).matcher(s);if(!m.matches())throw new IOException("来源未返回 JSON");s=m.group(1).trim();}if(!s.startsWith("{")&&!s.startsWith("["))throw new IOException("来源未返回 JSON 对象");JSONTokener tokener=new JSONTokener(s);Object parsed=tokener.nextValue();if(tokener.nextClean()!=0)throw new IOException("JSON 包含额外内容");return parsed;}
    public static byte[] decode64Bytes(String value)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();String alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";int bits=0,buffer=0;for(char ch:value.toCharArray()){if(ch=='=')break;if(Character.isWhitespace(ch))continue;int n=alphabet.indexOf(ch=='-'?'+':ch=='_'?'/':ch);if(n<0)throw new IOException("无效 Base64");buffer=(buffer<<6)|n;bits+=6;if(bits>=8){bits-=8;out.write((buffer>>bits)&255);}}return out.toByteArray();}
    public static String decode64(String value)throws Exception{return new String(decode64Bytes(value),"UTF-8");}
}
