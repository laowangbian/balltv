package cn.balltv.portal;
import java.util.*;
final class VerificationPolicy {
 static int sourceRank(String source){if(SourceCatalog.esports(source))return -1;if(source.equals("大米"))return 0;if(source.equals("看球通")||source.equals("So米"))return 1;if(source.equals("74体育/球吧")||source.equals("88看球"))return 2;if(source.equals("咖啡"))return 4;if(source.equals("JRKAN"))return 5;return 3;}
 static ArrayList<Feeds.Match> candidates(Events.Group group,String selected,Set<String> disabled,Set<String> tried){ArrayList<Feeds.Match> result=new ArrayList<Feeds.Match>();for(Feeds.Match m:group.members)if(!disabled.contains(m.source)&&(selected.equals("全部来源")||selected.equals(m.source))&&!tried.contains(m.source))result.add(m);Collections.sort(result,(a,b)->Integer.compare(sourceRank(a.source),sourceRank(b.source)));return result;}
}
