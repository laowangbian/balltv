package cn.balltv.portal;

import java.net.*;import java.util.*;import java.util.regex.*;import java.io.*;import org.jsoup.*;import org.jsoup.nodes.*;

/** Follow known public player wrappers without running scripts or evading access controls. */
public final class StreamResolver {
    public static boolean direct(String u){if(!HttpData.publicUrl(u))return false;try{return new URL(u).getPath().matches("(?i).*\\.(m3u8|mp4|flv|mpd)$");}catch(Exception e){return false;}}
    static String clean(String value){return ParserEntities.decode(value).replace("\\/","/").replace("\\u0026","&").trim();}
    public static String queryStream(String value){try{URL wrapper=new URL(value);String query=wrapper.getQuery();if(query==null)return "";for(String part:query.split("&")){int at=part.indexOf('=');if(at<0)continue;String key=part.substring(0,at);if(!key.equals("url")&&!key.equals("src")&&!key.equals("id")&&!key.equals("v"))continue;String v=URLDecoder.decode(part.substring(at+1),"UTF-8");if(v.startsWith("//"))v=wrapper.getProtocol()+":"+v;if(direct(v))return v;}}catch(Exception ignored){}return "";}
    interface PageLoader {HttpData.Reply get(String url,String referer)throws Exception;}
    public static String resolve(Feeds.Line line)throws Exception{return resolve(line,(url,ref)->HttpData.get(url,ref,false));}
    static String resolve(Feeds.Line line,PageLoader loader)throws Exception{String url=line.url,referer=line.referer;Set<String> visited=new HashSet<String>();
        for(int depth=0;depth<5;depth++){url=clean(url);if(url.startsWith("//"))url="https:"+url;if(direct(url)){line.referer=referer;return url;}String query=queryStream(url);if(!query.isEmpty()){line.referer=url;return query;}if(!visited.add(url))throw new IOException("播放器出现循环跳转");HttpData.Reply reply=loader.get(url,referer);Document doc=Jsoup.parse(reply.body,reply.url);
            for(Element element:doc.select("video[src],source[src]")){String candidate=Feeds.absolute(reply.url,element.attr("src"));if(direct(candidate)){line.referer=reply.url;return candidate;}}
            Matcher literal=Pattern.compile("(?:https?:)?(?://)[^\\s\"'<>\\\\]+\\.(?:m3u8|mp4|flv)(?:\\?[^\\s\"'<>\\\\]*)?",Pattern.CASE_INSENSITIVE).matcher(reply.body.replace("\\/","/").replace("\\u0026","&"));while(literal.find()){String candidate=clean(literal.group());if(candidate.startsWith("//"))candidate="https:"+candidate;if(direct(candidate)){line.referer=reply.url;return candidate;}}
            Element frame=doc.selectFirst("iframe[src]");if(frame!=null){referer=reply.url;url=Feeds.absolute(reply.url,frame.attr("src"));continue;}
            Matcher config=Pattern.compile("playerUrlBase64\\s*:\\s*[\"']([^\"']+)[\"']").matcher(reply.body),payload=Pattern.compile("(?:var|let|const)\\s+encodedStr\\s*=\\s*[\"']([A-Za-z0-9+/=]+)[\"']").matcher(reply.body);
            if(config.find()&&payload.find()){String prefix=HttpData.decode64(config.group(1));if(!HttpData.publicUrl(prefix)||!prefix.endsWith("id="))throw new IOException("播放器入口格式已变化");referer=reply.url;url=prefix+payload.group(1);continue;}
            if(reply.body.contains("XXTEA")&&reply.body.contains("encryptedBase64Str")){String queryPart=new URL(reply.url).getQuery();if(queryPart!=null)for(String part:queryPart.split("&"))if(part.startsWith("id=")){String encoded=URLDecoder.decode(part.substring(3).replace("+","%2B"),"UTF-8");line.referer=reply.url;return PlayerEnvelope.video(encoded,System.currentTimeMillis()/1000);}}
            // JRKAN's public sm wrapper builds /play/<numeric id>.html.
            String id="";try{String q=new URL(reply.url).getQuery();if(q!=null)for(String p:q.split("&"))if(p.matches("id=\\d+"))id=p.substring(3);}catch(Exception ignored){}
            if(!id.isEmpty()&&reply.body.contains("id1")&&reply.body.contains("/play/")){referer=reply.url;url=Feeds.absolute(reply.url,"/play/"+id+".html");continue;}
            Matcher assigned=Pattern.compile("(?:url|src)\\s*[:=]\\s*[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE).matcher(reply.body);String candidate="";while(assigned.find()){String v=clean(assigned.group(1));if(direct(v)){candidate=v;break;}}if(!candidate.isEmpty()){line.referer=reply.url;return candidate;}
            throw new IOException("该线路使用动态播放器，未返回可识别的视频地址；请换线");
        }throw new IOException("播放器嵌套过深，请换线");
    }
    private static final class ParserEntities {static String decode(String s){return org.jsoup.parser.Parser.unescapeEntities(s,false);}}
}
