package cn.balltv.portal;
import java.util.*;
/** Keep the original texture intact. Adapt only the cost of learned residual inference. */
final class NeuralPolicy {
 static boolean accepts(int w,int h,boolean hdr){return !hdr&&w>0&&h>0&&w<=4096&&h<=4096&&(long)w*h<=3840L*2160;}
 static int[][] levels(int w,int h){ArrayList<int[]> result=new ArrayList<int[]>();for(int[] bound:new int[][]{{1920,1080},{1280,720},{960,540},{640,360},{426,240}}){double scale=Math.min(1,Math.min(bound[0]/(double)w,bound[1]/(double)h));int x=Math.max(2,(int)Math.round(w*scale/2)*2),y=Math.max(2,(int)Math.round(h*scale/2)*2);if(result.isEmpty()||result.get(result.size()-1)[0]!=x||result.get(result.size()-1)[1]!=y)result.add(new int[]{x,y});}return result.toArray(new int[0][]);}
 static int initial(int[][] levels){for(int i=0;i<levels.length;i++)if(levels[i][0]<=1280&&levels[i][1]<=720)return i;return 0;}
 static double budget(double measured,double declared){double fps=Math.max(measured,declared>=10&&declared<=240?declared:0);if(fps<10||fps>240)fps=30;return Math.min(45,900./fps);}
 static final class Monitor {
  final double[] samples=new double[8];int count,index,slowWindows,fastWindows;long lastChange;boolean reduced;
  void changed(long now,boolean down){count=index=slowWindows=fastWindows=0;lastChange=now;reduced|=down;}
  double average(){if(count==0)return 0;double sum=0;for(int i=0;i<count;i++)sum+=samples[i];return sum/count;}
  // 1 reduces cost, -1 increases detail, 2 stops only after sustained overload at minimum cost.
  int sample(double ms,double budget,long now,boolean canReduce,boolean canIncrease,boolean drops){samples[index++%samples.length]=ms;count=Math.min(samples.length,count+1);if(count<5||now-lastChange<2500)return 0;double average=average();boolean slow=average>budget*1.12||drops;slowWindows=slow?slowWindows+1:0;fastWindows=!slow&&average<budget*.48?fastWindows+1:0;if(slowWindows>=2){if(canReduce)return 1;if(slowWindows>=6&&average>budget*1.35||slowWindows>=8&&drops)return 2;}if(canIncrease&&!reduced&&fastWindows>=3&&now-lastChange>=6000)return -1;return 0;}
 }
}
