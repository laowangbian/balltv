package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.BitmapDrawable;import android.media.MediaMetadataRetriever;import android.widget.*;import android.util.LruCache;import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
/** Static card images: bounded downloads, two workers, small decoded bitmaps. */
final class Thumbnails {
 final Semaphore decoder=VideoEvidence.DECODER;
 final ThreadLocal<Long> budget=new ThreadLocal<Long>();
 final ThreadLocal<Boolean> decoderBusy=new ThreadLocal<Boolean>();
 final MainActivity owner;
 final boolean lean=Ui.LEAN;
 final ExecutorService workers=BackgroundWork.pool(lean?1:2,"previews");
 final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(lean?8:32);
 final Map<ImageView,Long> displayedAt=new WeakHashMap<ImageView,Long>();
 final Map<String,Long> cachedAt=new HashMap<String,Long>();
 final Set<String> pending=new HashSet<String>();
 final LruCache<String,Long> failed=new LruCache<String,Long>(64);
 final Map<String,List<ImageView>> waiting=new HashMap<String,List<ImageView>>();
 final Map<ImageView,Events.Group> bound=new LinkedHashMap<ImageView,Events.Group>();
 volatile boolean closed;

 Thumbnails(MainActivity a){
  owner=a;
  workers.execute(()->{
   File[] files=a.getCacheDir().listFiles((dir,name)->name.startsWith("thumb10safe-")||name.startsWith("thumb14-")||name.startsWith("thumb15-"));
   if(files!=null){Arrays.sort(files,(x,y)->Long.compare(y.lastModified(),x.lastModified()));for(int i=64;i<files.length;i++)files[i].delete();}
  });
 }

 void bind(ImageView image,Events.Group group){
  boolean first=bound.put(image,group)==null;
  String key=PreviewFrames.key(group);
  Object previousKey=image.getTag();
  image.setTag(key);
  Bitmap bitmap=memory.get(key);
  Long cached=cachedAt.get(key);
  long now=System.currentTimeMillis();
  Long displayed=displayedAt.get(image);
  if(bitmap==null&&key.equals(previousKey)&&displayed!=null&&now-displayed<600000)return;
  if(bitmap!=null){
   if(!(image.getDrawable() instanceof BitmapDrawable)||((BitmapDrawable)image.getDrawable()).getBitmap()!=bitmap)image.setImageBitmap(bitmap);
   if(cached!=null&&now-cached<600000){displayedAt.put(image,cached);return;}
  }
  if(closed||!owner.foreground)return;
  if(image.getWidth()==0){
   if(first)image.addOnLayoutChangeListener(new android.view.View.OnLayoutChangeListener(){
    public void onLayoutChange(android.view.View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){
     if(r>l){v.removeOnLayoutChangeListener(this);if(!closed&&bound.containsKey(image))bind(image,bound.get(image));}
    }
   });
   return;
  }
  android.graphics.Rect visible=new android.graphics.Rect();
  if(!image.getGlobalVisibleRect(visible)||visible.height()<12)return;
  boolean library=!owner.view.equals("live");
  if((lean||!library)&&group.lead().status.equals("upcoming")&&group.start>now)return;
  if(pending.contains(key)){waitForImage(key,image);return;}
  if(lean&&(!pending.isEmpty()||!owner.quietForImages()))return;
  Long previousFailure=failed.get(key);
  if(previousFailure!=null&&now-previousFailure<(lean?45000:20000)){
   boolean newerSignal=false;
   for(Feeds.Match member:group.members){
    StreamHealth.Good good=StreamHealth.good(member,now);
    if(good!=null&&good.at>previousFailure){newerSignal=true;break;}
   }
   if(!newerSignal)return;
  }
  waitForImage(key,image);
  pending.add(key);
  workers.execute(()->loadImage(group,key,library));
 }

 void waitForImage(String key,ImageView image){
  List<ImageView> views=waiting.get(key);
  if(views==null){views=new ArrayList<ImageView>();waiting.put(key,views);}
  if(!views.contains(image))views.add(image);
 }

 void loadImage(Events.Group group,String key,boolean library){
  Bitmap result=null;
  boolean attempted=false;
  decoderBusy.set(false);
  budget.set(System.nanoTime()+(lean?7500000000L:11000000000L));
  try{
   if(owner.foreground&&!closed){
    File file=PreviewFrames.file(owner,group);
    if(file.exists()&&System.currentTimeMillis()-file.lastModified()<(library?7*86400000L:86400000L)){
     BitmapFactory.Options options=new BitmapFactory.Options();
     options.inPreferredConfig=lean?Bitmap.Config.RGB_565:Bitmap.Config.ARGB_8888;
     result=BitmapFactory.decodeFile(file.toString(),options);
     if(lean&&result!=null)result=scale(result);
    }
    if(result==null&&canLoad()&&!(group.lead().status.equals("upcoming")&&group.start>System.currentTimeMillis())){
     attempted=true;
     result=load(group);
     if(result!=null)PreviewFrames.store(owner,group,result,group.lead().sport);
    }
   }
  }catch(Exception ignored){}
  Bitmap ready=result;
  boolean tried=attempted&&!Boolean.TRUE.equals(decoderBusy.get());
  budget.remove();decoderBusy.remove();
  owner.runOnUiThread(()->{
   pending.remove(key);
   List<ImageView> views=waiting.remove(key);
   if(closed){if(ready!=null)ready.recycle();return;}
   if(ready!=null){
    memory.put(key,ready);
    cachedAt.keySet().retainAll(memory.snapshot().keySet());
    cachedAt.put(key,System.currentTimeMillis());
    failed.remove(key);
    if(views!=null)for(ImageView view:views)if(bound.containsKey(view)&&key.equals(view.getTag())){view.setImageBitmap(ready);displayedAt.put(view,System.currentTimeMillis());}
   }else if(tried&&owner.quietForImages())failed.put(key,System.currentTimeMillis());
   if(lean&&(ready!=null||tried)&&owner.quietForImages())refreshViews();
  });
 }
 Bitmap load(Events.Group group)throws Exception{
 ArrayList<Feeds.Match> choices=SourcePreferences.filter(owner,group.members);Collections.sort(choices,(x,y)->Boolean.compare(StreamHealth.good(x,System.currentTimeMillis())==null,StreamHealth.good(y,System.currentTimeMillis())==null));
 long full=budget.get();budget.set(Math.min(full,System.nanoTime()+2500000000L));Set<String> posters=new HashSet<String>();int attempts=0;
 for(Feeds.Match m:choices){if(owner.view.equals("live"))break;if(!canLoad())break;if(!posterUrl(m.cover)||!posters.add(m.cover))continue;if(attempts++>=4)break;try{Bitmap poster=decode(download(m.cover,Feeds.base(m.source)+"/",900000));if(poster!=null){if(PreviewFrames.acceptable(poster,m.sport))return poster;poster.recycle();}}catch(Exception ignored){}}
 budget.set(full);Set<String> urls=new HashSet<String>();attempts=0;
 for(Feeds.Match m:choices){if(!canLoad())return null;StreamHealth.Good good=StreamHealth.good(m,System.currentTimeMillis());if(good==null||!urls.add(good.url))continue;android.util.Log.d("BallTVCover","media "+m.sport+" "+(good.fingerprint.equals("flv")?"flv":"hls"));if(attempts++>=(lean?1:2))break;try{String ref=good.line.referer.isEmpty()?Feeds.base(m.source)+"/":good.line.referer;byte[] shared=ProbeFrames.take(StreamHealth.key(m),System.currentTimeMillis());if(shared!=null){try{Bitmap evidence=good.fingerprint.equals("flv")?acceptedFlv(shared,m.sport):fromSample(shared,good.initUrl,ref,m.sport);if(evidence!=null)return evidence;}catch(Exception ignored){}}Bitmap result=lean?null:good.fingerprint.equals("flv")?acceptedFlv(download(good.url,ref,2500000),m.sport):!good.previewUrl.isEmpty()&&System.currentTimeMillis()-good.at<30000?segment(good.previewUrl,good.initUrl,ref,m.sport):frame(good.url,ref,0,m.sport,m.kind.equals("replay"));if(result!=null)return result;}catch(Exception ignored){android.util.Log.d("BallTVCover","media miss "+ignored.getClass().getSimpleName());}if(canLoad()){try{VideoEvidence.Result evidence=VideoEvidence.capture(owner,good.url,good.line.referer.isEmpty()?Feeds.base(m.source)+"/":good.line.referer,m.sport,()->!closed&&owner.quietForImages(),lean?4500:6000);if(!evidence.attempted)decoderBusy.set(true);if(evidence.cover!=null)return evidence.cover;}catch(Exception ignored){}}}
 if(!owner.view.equals("live")&&canLoad())for(Feeds.Match m:choices)try{ArrayList<Feeds.Line> lines=Feeds.lines(m);if(lines.isEmpty())continue;Feeds.Line line=lines.get(0);return frame(Feeds.resolve(m,line),line.referer.isEmpty()?Feeds.base(m.source)+"/":line.referer,0,m.sport,m.kind.equals("replay"));}catch(Exception ignored){break;}
 return null;
 }
 boolean canLoad(){Long end=budget.get();return !closed&&owner.quietForImages()&&(end==null||System.nanoTime()<end);}
 static boolean posterUrl(String url){try{return HttpData.publicUrl(url)&&new URL(url).getPath().length()>1;}catch(Exception e){return false;}}
 Bitmap fromSample(byte[] bytes,String init,String ref,String sport)throws Exception{if(!init.isEmpty()){byte[] prefix=download(init,ref,250000),joined=new byte[prefix.length+bytes.length];System.arraycopy(prefix,0,joined,0,prefix.length);System.arraycopy(bytes,0,joined,prefix.length,bytes.length);bytes=joined;}Bitmap image=extract(bytes,sport);if(image!=null&&!PreviewFrames.frameAvailable(image)){image.recycle();return null;}return image;}
 Bitmap segment(String url,String init,String ref,String sport)throws Exception{byte[] prefix=init.isEmpty()?new byte[0]:download(init,ref,250000),media=download(url,ref,3200000);byte[] bytes=media;if(prefix.length>0){bytes=new byte[prefix.length+media.length];System.arraycopy(prefix,0,bytes,0,prefix.length);System.arraycopy(media,0,bytes,prefix.length,media.length);}Bitmap result=extract(bytes,sport);if(result!=null&&!PreviewFrames.frameAvailable(result)){android.util.Log.d("BallTVCover","frame rejected "+sport);result.recycle();return null;}return result;}
 Bitmap acceptedFlv(byte[] bytes,String sport)throws Exception{byte[] mp4=FlvPreview.mp4(bytes);if(mp4==null)return null;Bitmap result=extract(mp4,sport);if(result!=null&&!PreviewFrames.frameAvailable(result)){result.recycle();return null;}return result;}
 Bitmap frame(String url,String referer,int depth,String sport,boolean replay)throws Exception{
  if(depth>2)return null;
  if(url.toLowerCase(Locale.ROOT).matches(".*\\.(flv|mp4)(\\?.*)?")){byte[] bytes=download(url,referer,2500000);Bitmap image=extract(bytes.length>3&&bytes[0]=='F'&&bytes[1]=='L'&&bytes[2]=='V'?FlvPreview.mp4(bytes):bytes,sport);if(image!=null&&!PreviewFrames.frameAvailable(image)){image.recycle();image=null;}return image;}String text=new String(download(url,referer,180000),"UTF-8");if(!replay&&text.contains("#EXT-X-ENDLIST"))return null;PreviewPlan plan=PreviewPlan.parse(url,text);if(plan==null)return null;return plan.master?frame(plan.media,referer,depth+1,sport,replay):segment(plan.media,plan.init,referer,sport);
 }
 Bitmap extract(byte[] bytes,String sport)throws Exception{if(bytes==null||!canLoad()||!decoder.tryAcquire(500,TimeUnit.MILLISECONDS))return null;File clip=null;MediaMetadataRetriever retriever=null;try{clip=File.createTempFile("frame-",".ts",owner.getCacheDir());retriever=new MediaMetadataRetriever();try(FileOutputStream out=new FileOutputStream(clip)){out.write(bytes);}retriever.setDataSource(clip.toString());
  if(lean){
   String width=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
   String height=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
   if(width==null||height==null||(long)Integer.parseInt(width)*Integer.parseInt(height)>1280L*720)return null;
  }
  Bitmap image=android.os.Build.VERSION.SDK_INT>=27?retriever.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,180):retriever.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);if(image==null)image=retriever.getFrameAtTime(1000000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);Bitmap first=scale(image);return first;}finally{try{if(retriever!=null)retriever.release();}finally{if(clip!=null)clip.delete();decoder.release();}}}
 byte[] download(String url,String referer,int max)throws Exception{for(int redirect=0;redirect<4;redirect++){if(!HttpData.publicUrl(url)||!canLoad())throw new IOException();HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(1500);c.setReadTimeout(1500);c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("User-Agent",Feeds.UA);c.setRequestProperty("Referer",referer);try{int code=c.getResponseCode();if(code>=300&&code<400){url=Feeds.absolute(url,c.getHeaderField("Location"));continue;}if(code!=200&&code!=206)throw new IOException();try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[8192];int n;long end=System.currentTimeMillis()+6000;while((n=in.read(bytes))!=-1){if(System.currentTimeMillis()>end||!canLoad())throw new IOException("preview budget");int take=Math.min(n,max-out.size());out.write(bytes,0,take);if(out.size()>65536&&out.size()%65536<8192&&url.toLowerCase(Locale.ROOT).contains(".flv")&&FlvPreview.find(out.toByteArray())!=null)break;if(out.size()>=max)break;}return out.toByteArray();}}finally{c.disconnect();}}throw new IOException();}
 Bitmap decode(byte[] bytes){BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<=0||bounds.outWidth>16000||bounds.outHeight>16000)return null;BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=Math.max(1,Math.max(bounds.outWidth/480,bounds.outHeight/270));opts.inPreferredConfig=Bitmap.Config.ARGB_8888;return scale(BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts));}
 Bitmap scale(Bitmap b){if(b==null)return null;Bitmap small=Bitmap.createScaledBitmap(b,lean?192:320,lean?108:180,true);
  if(small!=b)b.recycle();
  if(lean&&small.getConfig()!=Bitmap.Config.RGB_565){Bitmap compact=small.copy(Bitmap.Config.RGB_565,false);if(compact!=null){small.recycle();small=compact;}}
  return small;}
 void refreshViews(){for(Map.Entry<ImageView,Events.Group> e:new ArrayList<Map.Entry<ImageView,Events.Group>>(bound.entrySet()))if(e.getKey()!=null)bind(e.getKey(),e.getValue());}
 void clearViews(){waiting.clear();bound.clear();}
 void onReturn(String key){
  if(key==null)return;
  failed.remove(key);memory.remove(key);cachedAt.remove(key);
  for(ImageView image:bound.keySet())if(key.equals(image.getTag()))displayedAt.remove(image);
 }
 void close(){closed=true;workers.shutdownNow();waiting.clear();}
}
