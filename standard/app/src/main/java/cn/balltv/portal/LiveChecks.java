package cn.balltv.portal;
import java.util.*;import java.util.concurrent.*;
/** UI-owned fair queue. No executor backlog; each source attempt shares one deadline. */
final class LiveChecks {
 final MainActivity owner;final int limit=Ui.LEAN?2:3;final ExecutorService pool=BackgroundWork.pool(limit,"signals");
 final Set<String> pending=new HashSet<String>();final Map<String,Job> jobs=new LinkedHashMap<String,Job>();final ArrayDeque<Job> queue=new ArrayDeque<Job>();
 final Map<String,Integer> failureRuns=new HashMap<String,Integer>();final Map<String,Long> attempted=new HashMap<String,Long>();final Map<String,String> failures=new LinkedHashMap<String,String>(),signatures=new HashMap<String,String>();
 final Map<String,long[]> timings=new LinkedHashMap<String,long[]>();Set<String> current=new HashSet<String>();int running;volatile boolean closed;
 static final class Job {final String key,source;volatile Events.Group group;final Set<String> tried=new HashSet<String>();volatile boolean cancelled;boolean working,decoderTried;Job(String key,String source,Events.Group group){this.key=key;this.source=source;this.group=group;}}
 LiveChecks(MainActivity owner){this.owner=owner;}
 String key(Events.Group group){return group.id+"|"+owner.source;}
 boolean ready(Events.Group group,long maxAge){long now=System.currentTimeMillis(),at=StreamHealth.available(group,owner.source,SourcePreferences.disabled(owner),now);return at>0&&now-at<maxAge;}
 boolean ensure(Events.Group group){return ready(group,90001);}
 void sync(List<Events.Group> groups){
  Set<String> wanted=new HashSet<String>();for(Events.Group g:groups)wanted.add(key(g));current=wanted;
  Iterator<Job> obsolete=jobs.values().iterator();while(obsolete.hasNext()){Job job=obsolete.next();if(!wanted.contains(job.key)){job.cancelled=true;pending.remove(job.key);queue.remove(job);obsolete.remove();}}
  for(Events.Group group:groups){String key=key(group);if(ready(group,45000)){Job job=jobs.remove(key);if(job!=null){job.cancelled=true;queue.remove(job);}pending.remove(key);failures.remove(key);continue;}
   Job existing=jobs.get(key);if(existing!=null){existing.group=group;continue;}
   TreeSet<String> sites=new TreeSet<String>();for(Feeds.Match m:group.members)sites.add(m.source);String signature=sites.toString();boolean changed=!signature.equals(signatures.get(key));signatures.put(key,signature);
   Long last=attempted.get(key);if(!changed&&last!=null&&System.currentTimeMillis()-last<20000L*Math.min(3,failureRuns.containsKey(key)?failureRuns.get(key):1))continue;
   Job job=new Job(key,owner.source,group);jobs.put(key,job);pending.add(key);queue.addLast(job);
  }
  failures.keySet().retainAll(wanted);pump();
 }
 void pump(){
  while(!closed&&owner.foreground&&running<limit&&!queue.isEmpty()){
   Job job=queue.removeFirst();if(job.cancelled||jobs.get(job.key)!=job)continue;
   ArrayList<Feeds.Match> candidates=VerificationPolicy.candidates(job.group,job.source,SourcePreferences.disabled(owner),job.tried);
   if(candidates.isEmpty()||job.tried.size()>=4){finish(job);continue;}
   Collections.sort(candidates,(a,b)->Integer.compare(sourceScore(a.source),sourceScore(b.source)));Feeds.Match match=candidates.get(0);job.tried.add(match.source);job.working=true;running++;
   pool.execute(()->probe(job,match));
  }
 }
 boolean valid(Job job,Feeds.Match match){return !closed&&!job.cancelled&&owner.foreground&&SourcePreferences.enabled(owner,match.source)&&!RequestBudget.expired();}
 void probe(Job job,Feeds.Match match){
  long begin=System.nanoTime();boolean accepted=false;String reason="暂未取得可播放信号";RequestBudget.begin(7500);
  try{
   if(valid(job,match)){
    ArrayList<Feeds.Line> lines=new ArrayList<Feeds.Line>();StreamHealth.Good cached=StreamHealth.good(match,System.currentTimeMillis());if(cached!=null&&System.currentTimeMillis()-cached.at>=20000)cached=null;if(cached!=null)lines.add(cached.line);
    if(lines.isEmpty())lines=Feeds.lines(match);
    Collections.sort(lines,(a,b)->Integer.compare(StreamHealth.rank(match,a,System.currentTimeMillis()),StreamHealth.rank(match,b,System.currentTimeMillis())));
    int count=0;for(Feeds.Line line:lines){if(!valid(job,match)||count>=2)break;if(StreamHealth.bad(match,line,System.currentTimeMillis()))continue;count++;
     boolean decoderBusy=false;String resolved="";try{String url=cached!=null&&cached.line==line?cached.url:Feeds.resolve(match,line);resolved=url;if(!valid(job,match))break;SignalProbe.Result result=SignalProbe.check(url,line.referer.isEmpty()?Feeds.base(match.source)+"/":line.referer);if(valid(job,match)&&StreamHealth.accept(match,line,url,result,System.currentTimeMillis())){accepted=true;break;}}
     catch(Exception error){reason=describe(error);if(!resolved.isEmpty()&&!(error.getMessage()!=null&&error.getMessage().contains("播放列表已结束"))&&!job.decoderTried&&!job.cancelled&&owner.foreground){job.decoderTried=true;RequestBudget.clear();try{VideoEvidence.Result evidence=VideoEvidence.capture(owner,resolved,line.referer.isEmpty()?Feeds.base(match.source)+"/":line.referer,match.sport,()->!closed&&!job.cancelled&&owner.foreground,7000);if(!evidence.attempted){job.decoderTried=false;decoderBusy=true;}if(evidence.playing&&!job.cancelled&&owner.foreground){StreamHealth.playing(match,line,resolved,System.currentTimeMillis());if(evidence.cover!=null){try{PreviewFrames.store(owner,job.group,evidence.cover,match.sport);}finally{evidence.cover.recycle();}}accepted=true;break;}if(evidence.cover!=null)evidence.cover.recycle();}catch(Exception ignored){}finally{RequestBudget.begin(1000);}}if(!decoderBusy&&valid(job,match))StreamHealth.fail(match,line,System.currentTimeMillis());}
    }
   }
  }catch(Exception error){reason=NetworkIssue.describe(error);}finally{RequestBudget.clear();}
  final boolean good=accepted;final String issue=reason;final long ms=(System.nanoTime()-begin)/1000000;
  owner.runOnUiThread(()->{running--;job.working=false;long[] stats=timings.get(match.source);if(stats==null){stats=new long[3];timings.put(match.source,stats);}stats[0]++;stats[1]+=ms;if(good)stats[2]++;android.util.Log.i("BallTVCheck",SourceCatalog.label(match.source)+" "+(good?"pass":"miss")+" "+ms+"ms");
   if(!closed&&!job.cancelled&&jobs.get(job.key)==job){if(good){failures.remove(job.key);finish(job);}else{failures.put(job.key,SourceCatalog.label(match.source)+"："+issue);queue.addLast(job);}}
   pump();if(!closed&&!owner.dead)owner.renderSoon();
  });
 }
 void finish(Job job){if(failures.containsKey(job.key)&&!ready(job.group,90001))failureRuns.put(job.key,Math.min(3,(failureRuns.containsKey(job.key)?failureRuns.get(job.key):0)+1));else failureRuns.remove(job.key);jobs.remove(job.key);pending.remove(job.key);queue.remove(job);attempted.put(job.key,System.currentTimeMillis());}
 int activeCount(){int count=0;for(Job job:jobs.values())if(job.working&&!job.cancelled)count++;return count;}
 int waitingCount(){return Math.max(0,jobs.size()-activeCount());}
 int failedCount(){int n=0;for(String k:failures.keySet())if(current.contains(k)&&!pending.contains(k))n++;return n;}
 int sourceScore(String source){int score=VerificationPolicy.sourceRank(source)*100;long[] s=timings.get(source);if(s!=null&&s[0]>=3){if(s[2]*4<s[0])score+=500;if(s[1]/s[0]>5000)score+=200;else if(s[2]*2>=s[0]&&s[1]/s[0]<2000)score-=100;}return score;}
 static String describe(Exception error){String message=error.getMessage()==null?"":error.getMessage();if(message.contains("加密"))return "此加密格式尚未通过轻量核验";if(message.contains("播放列表已结束"))return "上游播放列表已结束";if(message.contains("视频分片"))return "本次未取得可识别的视频分片";if(message.contains("FLV 视频帧"))return "本次未取得连续 FLV 视频帧，可重试";return NetworkIssue.describe(error);}
 void reset(){pause();attempted.clear();failureRuns.clear();failures.clear();signatures.clear();current.clear();}
 String report(){StringBuilder out=new StringBuilder("正在核验："+activeCount()+" 场\n排队等待："+waitingCount()+" 场\n暂未取得信号："+failedCount()+" 场\n\n目录“已连接”仅表示已取得站点赛事列表，不代表该站点每个房间都能播放。先检查实际媒体数据；轻量探测无法判断时，用静音解码检查真实视频帧。未取得有效信号的房间不在首页展示，稍后开赛也需取得视频信号。每场比赛先尝试一个来源，再轮换到其他比赛；请求共享时限，慢来源不会占用无限队列。\n\n来源核验（本次会话，按来源尝试计数）：\n");for(Map.Entry<String,long[]> entry:timings.entrySet()){long[] s=entry.getValue();out.append(SourceCatalog.label(entry.getKey())).append("：").append(s[2]).append("/").append(s[0]).append(" 次通过，均值 ").append(s[1]/Math.max(1,s[0])).append("ms\n");}int n=0;for(Map.Entry<String,String> failure:failures.entrySet())if(current.contains(failure.getKey())&&!pending.contains(failure.getKey())&&n++<8)out.append("\n").append(failure.getValue());return out.toString();}
 void pause(){for(Job job:jobs.values())job.cancelled=true;jobs.clear();pending.clear();queue.clear();}
 void close(){closed=true;pause();pool.shutdownNow();}
}
