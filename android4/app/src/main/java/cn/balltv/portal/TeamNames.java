package cn.balltv.portal;
import java.util.*;import org.json.*;
/** Explicit translations and names co-published on one team object; no score/time fuzzy guesses. */
final class TeamNames {
 static final Map<String,String> ALIASES=new HashMap<String,String>();
 static {group("CSJL骑士","CSJL Knight","CSJL Knights","Letran Knights","莱特兰骑士","CSJL骑士");group("UPHSD阿特拉斯","UPHSD Altas","UPHSD阿尔塔斯","UPHSD阿特拉斯","Perpetual Altas","永久阿尔塔斯","永久阿尔塔斯");group("河内多毛公牛","Hanoi Da Mau Bulls","Hanoi Da Mau","河内公牛","河内多毛公牛","河内多毛公牛队");group("澳门黑骑士","Macau Black Knights","Macau Black","Macau黑骑士","澳门黑骑士");}
 static String clean(String name){return java.text.Normalizer.normalize(name,java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\s·•._’']","");}
 static synchronized String canonical(String name){String key=clean(name);String value=ALIASES.get(key);return value==null?key:value;}
 static synchronized void group(String canonical,String...names){String root=clean(canonical);String existing=ALIASES.get(root);if(existing!=null)root=existing;Set<String> roots=new HashSet<String>();for(String name:names){String r=ALIASES.get(clean(name));if(r!=null)roots.add(r);}for(Map.Entry<String,String> e:ALIASES.entrySet())if(roots.contains(e.getValue()))e.setValue(root);ALIASES.put(clean(canonical),root);for(String name:names)if(name!=null&&!name.trim().isEmpty())ALIASES.put(clean(name),root);}
 static void learn(JSONObject object){learn(object,0,false);}
 static void learn(JSONObject o,int depth,boolean team){if(o==null||depth>4)return;
  for(String prefix:new String[]{"home","away","host","guest","home_team","away_team","host_team","guest_team","teamA","teamB"}){
   ArrayList<String> names=new ArrayList<String>();for(String suffix:new String[]{"_name_zh","_name_en","_name","NameZh","NameEn","Name","_zh","_en"})add(names,o.opt(prefix+suffix));if(!names.isEmpty()){add(names,o.opt(prefix));if(names.size()>1)group(names.get(0),names.toArray(new String[0]));}}
  if(team){ArrayList<String> names=new ArrayList<String>();for(String key:new String[]{"name_zh","name_en","name_cn","nameZh","nameEn","name","short_name_zh","short_name_en"})add(names,o.opt(key));if(names.size()>1)group(names.get(0),names.toArray(new String[0]));}
  Iterator<String> keys=o.keys();while(keys.hasNext()){String key=keys.next();Object child=o.opt(key);if(child instanceof JSONObject)learn((JSONObject)child,depth+1,key.toLowerCase(Locale.ROOT).matches(".*(home|away|host|guest|team).*"));}
 }
 static void add(List<String> names,Object value){if(value instanceof String){String text=((String)value).trim();if(text.length()>1&&text.length()<100&&!text.equals("null")&&!text.matches("[0-9]+"))names.add(text);}}
}
