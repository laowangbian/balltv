package cn.balltv.portal;
/** Time is a fallback, never proof that the match has started. */
public final class StartGate {
 public static boolean open(long kickoff,long now,boolean videoReady,int consecutiveFieldFrames){return videoReady&&(consecutiveFieldFrames>=2||kickoff>0&&now>=kickoff-180000L);}
}
