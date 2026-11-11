package cn.balltv.portal;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class VerificationPolicyTest {
 Feeds.Match match(String source){Feeds.Match m=new Feeds.Match();m.source=source;m.id=source;m.home="甲队";m.away="乙队";m.sport="basketball";m.time="2026-11-10 08:00";return m;}
 Events.Group group(String... names){Events.Group g=new Events.Group(match(names[0]));for(int i=1;i<names.length;i++)g.members.add(match(names[i]));return g;}
 @Test public void newlyArrivedDirectSourceCanPassWaitingNestedWrappers(){Events.Group g=group("咖啡","JRKAN");g.members.add(match("大米"));assertEquals("大米",VerificationPolicy.candidates(g,"全部来源",Collections.<String>emptySet(),Collections.<String>emptySet()).get(0).source);}
 @Test public void explicitSourceDisabledAndAlreadyAttemptedSourcesAreRespected(){Events.Group g=group("咖啡","JRKAN","大米");assertEquals("JRKAN",VerificationPolicy.candidates(g,"JRKAN",Collections.<String>emptySet(),Collections.<String>emptySet()).get(0).source);assertTrue(VerificationPolicy.candidates(g,"咖啡",new HashSet<String>(Arrays.asList("咖啡")),Collections.<String>emptySet()).isEmpty());assertEquals("咖啡",VerificationPolicy.candidates(g,"全部来源",Collections.<String>emptySet(),new HashSet<String>(Arrays.asList("大米"))).get(0).source);}
}
