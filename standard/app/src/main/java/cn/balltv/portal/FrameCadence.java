package cn.balltv.portal;
import java.util.Arrays;
/** Presentation timestamps, not picture sharpness or wall-clock rendering throughput. */
final class FrameCadence {
 final long[] intervals=new long[120];int count,index;long previous=Long.MIN_VALUE;
 synchronized void reset(){count=index=0;previous=Long.MIN_VALUE;}
 synchronized void observe(long pts){if(previous!=Long.MIN_VALUE){long delta=pts-previous;if(delta>=4000&&delta<=200000){intervals[index++%intervals.length]=delta;count=Math.min(count+1,intervals.length);}else if(delta<0||delta>1000000){count=index=0;}}previous=pts;}
 synchronized double fps(){if(count<6)return 0;long[] sorted=Arrays.copyOf(intervals,count);Arrays.sort(sorted);int trim=count>=10?count/10:0;double sum=0;int stable=0;for(int i=trim;i<sorted.length-trim;i++){sum+=sorted[i];stable++;}return stable==0?0:1000000.*stable/sum;}
}
