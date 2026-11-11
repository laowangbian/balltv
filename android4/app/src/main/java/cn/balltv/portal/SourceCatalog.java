package cn.balltv.portal;

/** One registry for UI, transport and capability reporting. Mirrors are not sources. */
public final class SourceCatalog {
    public static final String[] ALL={BiliEsports.NAME,HuyaEsports.NAME,KuaishouEsports.NAME,"178直播","看球通","74体育/球吧",DamiSource.NAME,"咖啡","豆包",ExtraSources.SOMI,"88看球","雨燕/7Live","JRKAN",SeriesSource.NAME,RoomSources.LONGMEN,RoomSources.JINGYU,TiantianSource.NAME,HaixingSource.NAME};
    static boolean inCategory(String name,String category){return esports(name)==category.equals("电竞");}
    static boolean esports(String name){return name.equals(BiliEsports.NAME)||name.equals(HuyaEsports.NAME)||name.equals(KuaishouEsports.NAME);}
    public static String label(String name){if(name.equals(BiliEsports.NAME))return "B站";return name.replace(SeriesSource.NAME,"玩球等站").replace("雨燕/7Live","雨燕/87");}
    public static boolean supports(String name,String view){
        if(view.equals("favorites"))return true;if(esports(name)||name.equals(HaixingSource.NAME))return view.equals("live");if((name.equals(SeriesSource.NAME)||RoomSources.handles(name)||name.equals(TiantianSource.NAME))&&view.equals("schedule"))return false;
        if(view.equals("replay"))return name.equals("咖啡")||name.equals("豆包")||name.equals("88看球")||name.equals("74体育/球吧");
        return true;
    }
    public static boolean additional(String name){return !name.equals("咖啡")&&!name.equals("豆包");}
}
