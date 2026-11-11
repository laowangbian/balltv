package cn.balltv.portal;
import java.util.*;
/** Limits automatic failures and source cycling independently of Android UI. */
public final class FailoverPolicy {
    final int maximum;final long started,window;int failures;final Set<Integer> visited=new HashSet<Integer>();
    public FailoverPolicy(int maximum,long window,long now){this.maximum=maximum;this.window=window;started=now;}
    public void visit(int source){visited.add(source);}
    public boolean failed(long now){failures++;return failures<maximum&&now-started<window;}
    public int nextSource(int current,int size){for(int step=1;step<=size;step++){int next=(current+step)%size;if(!visited.contains(next))return next;}return -1;}
    public int nextLine(int current,int size,int failedInSource){return failedInSource<2&&current+1<size?current+1:-1;}
}
