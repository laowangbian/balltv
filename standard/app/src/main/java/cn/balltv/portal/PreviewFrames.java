package cn.balltv.portal;
import android.content.Context;import android.graphics.*;import java.io.*;
/** Shares actual decoded player frames with all library cards. No signed URLs stored. */
final class PreviewFrames {
 static String key(Events.Group g){return PreviewPlan.stableKey(g);}
 static File file(Context c,Events.Group g){return new File(c.getCacheDir(),(g.lead().kind.equals("live")?"thumb16-live-":"thumb15-")+Integer.toHexString(key(g).hashCode())+".jpg");}
 static File legacyFile(Context c,Events.Group g){return new File(c.getCacheDir(),"thumb10safe-"+Integer.toHexString(("v10:"+g.id+":"+g.start).hashCode())+".jpg");}
 static int[] sample(Bitmap b){Bitmap tiny=Bitmap.createScaledBitmap(b,48,27,true);int[] p=new int[1296];tiny.getPixels(p,0,48,0,0,48,27);if(tiny!=b)tiny.recycle();return p;}
 static boolean acceptable(Bitmap b,String sport){return SportsPreview.acceptable(sample(b),sport);}
 static boolean frameAvailable(Bitmap b){return b!=null&&!b.isRecycled()&&b.getWidth()>0&&b.getHeight()>0;}
 static boolean store(Context c,Events.Group g,Bitmap b,String sport){if(!frameAvailable(b))return false;File target=file(c,g),temp=new File(target+".tmp");try{ synchronized(PreviewFrames.class){try(FileOutputStream out=new FileOutputStream(temp)){b.compress(Bitmap.CompressFormat.JPEG,82,out);}return temp.renameTo(target);}}catch(Exception e){temp.delete();return false;}}
}
