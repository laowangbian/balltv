package cn.balltv.portal;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;

final class Store {
    static JSONArray load(Context c){try{return new JSONArray(c.getSharedPreferences("tv",0).getString("sites",read(c.getAssets().open("sites.json"))));}catch(Exception e){return new JSONArray();}}
    static String read(InputStream in)throws IOException{try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1024*1024)throw new IOException("配置超过 1 MB");out.write(buf,0,n);}return out.toString("UTF-8");}finally{in.close();}}
    static JSONArray validate(String raw)throws Exception{JSONArray a=new JSONArray(raw);if(a.length()>300)throw new Exception("最多 300 个入口");Set<String> ids=new HashSet<String>();for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String id=o.getString("id");if(id.length()==0||!ids.add(id))throw new Exception("入口 id 为空或重复");if(o.getString("name").trim().length()==0)throw new Exception("名称为空");JSONArray urls=o.getJSONArray("urls");if(urls.length()==0||urls.length()>20)throw new Exception("每个入口需有 1–20 个地址");for(int j=0;j<urls.length();j++)if(!Ui.webUrl(urls.getString(j)))throw new Exception("只支持 http/https 地址");if(!"web".equals(o.optString("type","web"))&&!"video".equals(o.optString("type")))throw new Exception("type 需为 web 或 video");}return a;}
    static void save(Context c,JSONArray a){c.getSharedPreferences("tv",0).edit().putString("sites",a.toString()).apply();}
    static boolean fav(Context c,String id){return c.getSharedPreferences("tv",0).getBoolean("fav:"+id,false);}
    static void toggle(Context c,String id){c.getSharedPreferences("tv",0).edit().putBoolean("fav:"+id,!fav(c,id)).apply();}
}

