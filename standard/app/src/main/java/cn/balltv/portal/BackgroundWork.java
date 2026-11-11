package cn.balltv.portal;
import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;
/** Keep catalog parsing and preview decoders below the UI thread on old TV CPUs. */
final class BackgroundWork {
 static ExecutorService pool(int count,String label){AtomicInteger next=new AtomicInteger();return Executors.newFixedThreadPool(count,task->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);task.run();},"BallTV-"+label+"-"+next.incrementAndGet()));}
}
