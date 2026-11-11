package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class FailoverPolicyTest {
    @Test public void aFailedProviderMovesToUnvisitedProvider(){FailoverPolicy p=new FailoverPolicy(8,120000,0);p.visit(0);p.visit(1);assertEquals(2,p.nextSource(1,3));p.visit(2);assertEquals(-1,p.nextSource(2,3));}
    @Test public void badLinesCannotLoopForever(){FailoverPolicy p=new FailoverPolicy(3,120000,0);assertTrue(p.failed(1));assertTrue(p.failed(2));assertFalse(p.failed(3));}
    @Test public void timeBudgetStopsFurtherAutomaticRequests(){FailoverPolicy p=new FailoverPolicy(8,120000,0);assertFalse(p.failed(120000));}
    @Test public void twoFailuresSwitchSourceRatherThanWalkDozensOfLines(){FailoverPolicy p=new FailoverPolicy(8,120000,0);assertEquals(1,p.nextLine(0,20,1));assertEquals(-1,p.nextLine(1,20,2));}
    @Test public void oneSourceDoesNotCycleBackToItself(){FailoverPolicy p=new FailoverPolicy(8,120000,0);p.visit(0);assertEquals(-1,p.nextSource(0,1));}
}
