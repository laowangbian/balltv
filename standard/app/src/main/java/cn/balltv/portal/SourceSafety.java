package cn.balltv.portal;
import java.util.Locale;
final class SourceSafety {
 static boolean promotional(String text){if(text==null)return false;String s=text.toLowerCase(Locale.ROOT).replace(" ","");for(String word:new String[]{"荐彩","竞彩","北单","博彩","稳单","私信","加群","领取","福利","投注","下注","充值","彩金","返利","稳赚","包赢","推广","下载app","下载软件","推单","重心单","百家乐","棋牌","彩票","真钱","下注","包赔"})if(s.contains(word))return true;return false;}
 static boolean usable(Feeds.Match m){if(m.home.isEmpty()&&m.away.isEmpty()&&promotional(m.title+" "+m.league))return false;if(promotional(m.league))m.league="体育赛事";return true;}
}
