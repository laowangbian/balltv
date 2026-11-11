package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class StartGateTest {
 final long kickoff=Events.epoch("2026-11-10 00:30");
 @Test public void automaticallyRevealsThreeMinutesEarlyWhenReady(){assertFalse(StartGate.open(kickoff,kickoff-180001,true,0));assertTrue(StartGate.open(kickoff,kickoff-180000,true,0));assertTrue(StartGate.open(kickoff,kickoff,true,0));}
 @Test public void fieldEvidenceCanRevealEarlyButNeedsTwoSamples(){assertFalse(StartGate.open(kickoff,kickoff-600000,true,1));assertTrue(StartGate.open(kickoff,kickoff-600000,true,2));}
 @Test public void neverRevealsUnpreparedVideoOrUnknownTime(){assertFalse(StartGate.open(kickoff,kickoff,false,5));assertFalse(StartGate.open(0,kickoff,true,0));}
}
