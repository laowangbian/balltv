package cn.balltv.portal;
/** An entire verification turn shares a deadline across adapters and media probes. */
final class RequestBudget {
 private static final ThreadLocal<Long> END=new ThreadLocal<Long>();
 static void begin(long millis){END.set(System.nanoTime()+millis*1000000L);}
 static long deadline(long normal){Long end=END.get();return end==null?normal:Math.min(normal,end);}
 static boolean active(){return END.get()!=null;}
 static boolean expired(){Long end=END.get();return end!=null&&System.nanoTime()>=end;}
 static void clear(){END.remove();}
}
