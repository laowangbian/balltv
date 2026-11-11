package cn.balltv.portal;

import java.util.*;import java.text.*;

/** Conservative event grouping: known teams, same sport, starts within 45 min. */
public final class Events {
    private static final Map<String,String> NBA=new HashMap<String,String>();
    static {String[][] aliases={{"凯尔特人","波士顿凯尔特人"},{"篮网","布鲁克林篮网"},{"尼克斯","纽约尼克斯"},{"76人","费城76人","费城七十六人"},{"猛龙","多伦多猛龙"},{"公牛","芝加哥公牛"},{"骑士","克利夫兰骑士"},{"活塞","底特律活塞"},{"步行者","印第安纳步行者"},{"雄鹿","密尔沃基雄鹿"},{"老鹰","亚特兰大老鹰"},{"黄蜂","夏洛特黄蜂"},{"热火","迈阿密热火"},{"魔术","奥兰多魔术"},{"奇才","华盛顿奇才"},{"掘金","丹佛掘金"},{"森林狼","明尼苏达森林狼"},{"雷霆","俄克拉荷马城雷霆"},{"开拓者","波特兰开拓者"},{"爵士","犹他爵士"},{"勇士","金州勇士"},{"快船","洛杉矶快船"},{"湖人","洛杉矶湖人"},{"太阳","菲尼克斯太阳"},{"国王","萨克拉门托国王"},{"独行侠","达拉斯独行侠","达拉斯小牛"},{"火箭","休斯顿火箭"},{"灰熊","孟菲斯灰熊"},{"鹈鹕","新奥尔良鹈鹕"},{"马刺","圣安东尼奥马刺"}};for(String[] group:aliases)for(String name:group)NBA.put(name,group[0]);}
    static final java.util.regex.Pattern SPACE=java.util.regex.Pattern.compile("[\\s·•]");static final Map<String,String> CLEAN=new LinkedHashMap<String,String>(256,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>1024;}};static final Map<String,Long> TIMES=new LinkedHashMap<String,Long>(256,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Long> e){return size()>2048;}};
    static String clean(String name){if(name==null)return "";synchronized(CLEAN){String cached=CLEAN.get(name);if(cached!=null)return cached;String value=SPACE.matcher(name).replaceAll("");CLEAN.put(name,value);return value;}}
    public static String canonical(String name){String s=clean(name);return TeamNames.canonical(NBA.containsKey(s)?NBA.get(s):s);}
    public static String shortName(String name){String s=clean(name);return NBA.containsKey(s)?NBA.get(s):name;}
    public static String sport(String value){if(value.equals("1"))return"football";if(value.equals("2"))return"basketball";if(value.equals("3"))return"tennis";if(value.equals("19")||value.equals("billiards"))return"snooker";return value;}
    public static long epoch(String value){if(value==null)return 0;synchronized(TIMES){Long cached=TIMES.get(value);if(cached!=null)return cached;}long parsed=parseEpoch(value);synchronized(TIMES){TIMES.put(value,parsed);}return parsed;}
    static long parseEpoch(String value){if(value==null||value.trim().isEmpty())return 0;String s=value.trim();try{if(s.matches("\\d{10,13}")){long n=Long.parseLong(s);return n<100000000000L?n*1000:n;}boolean utc=s.endsWith("Z");s=s.replace('T',' ').replace("Z","").replaceAll("\\.\\d+$","");for(String pattern:new String[]{"yyyy-MM-dd HH:mm:ss","yyyy-MM-dd HH:mm"}){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone(utc?"UTC":"Asia/Shanghai"));try{return f.parse(s).getTime();}catch(ParseException ignored){}}}catch(Exception ignored){}return 0;}
    public static String when(String raw){long t=epoch(raw);if(t==0)return raw==null?"":raw;SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date(t));}
    static String identity(Feeds.Match m){String a=canonical(m.home),b=canonical(m.away);if(epoch(m.time)==0||sport(m.sport).isEmpty())return"";if(a.isEmpty()||b.isEmpty()||a.equals(b))return KuaishouEsports.eventIdentity(m);return sport(m.sport)+"|"+(a.compareTo(b)<=0?a+"|"+b:b+"|"+a);}
    public static final class Group {
        public final ArrayList<Feeds.Match> members=new ArrayList<Feeds.Match>();public final String identity,id;public final long start;
        Group(Feeds.Match m){identity=identity(m);start=epoch(m.time);id=identity.isEmpty()?m.key():m.kind+"|"+identity+"|"+EventOrder.dayKey(start);members.add(m);}
        public Feeds.Match lead(){Feeds.Match best=members.get(0);for(Feeds.Match m:members)if(!m.estimated&&m.home.length()>0&&(!m.score.trim().equals(":")&&!m.score.isEmpty())){best=m;break;}return best;}
        public String sources(){StringBuilder s=new StringBuilder();Set<String> seen=new HashSet<>();for(Feeds.Match m:members){if(!seen.add(m.source))continue;if(s.length()>0)s.append(" · ");s.append(SourceCatalog.label(m.source));}return s.toString();}
        public int sourceCount(){Set<String>s=new HashSet<String>();for(Feeds.Match m:members)s.add(m.source);return s.size();}
    }
    public static ArrayList<Group> group(Collection<Feeds.Match> matches){return group(matches,true);}
    public static ArrayList<Group> group(Collection<Feeds.Match> matches,boolean prioritizeSources){ArrayList<Group> groups=new ArrayList<Group>();Map<String,List<Group>> index=new HashMap<String,List<Group>>();Set<String> unique=new HashSet<String>();for(Feeds.Match m:matches){if(!unique.add(m.key()+"|"+m.time+"|"+m.home+"|"+m.away))continue;String identity=identity(m),key=identity+"|"+m.kind;Group chosen=null;List<Group> candidates=index.get(key);if(!identity.isEmpty()&&candidates!=null)for(Group g:candidates)if(Math.abs(epoch(m.time)-g.start)<=45*60000L){chosen=g;break;}if(chosen==null){chosen=new Group(m);groups.add(chosen);if(!identity.isEmpty()){if(candidates==null){candidates=new ArrayList<Group>();index.put(key,candidates);}candidates.add(chosen);}}else chosen.members.add(m);}if(prioritizeSources)Collections.sort(groups,(x,y)->{int count=Integer.compare(y.sourceCount(),x.sourceCount());return count!=0?count:Long.compare(x.start,y.start);});return groups;}

}
