package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;import android.util.LruCache;
/** Supersampled at the actual destination size; seams meet a clean ball outline. */
final class BrandMark extends Drawable {
 static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(8);
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
 public int getIntrinsicWidth(){return 64;}public int getIntrinsicHeight(){return 64;}
 public void draw(Canvas canvas){Rect b=getBounds();if(b.width()<=0||b.height()<=0)return;String key=b.width()+"x"+b.height();Bitmap image=CACHE.get(key);if(image==null){int w=Math.min(384,b.width()),h=Math.min(384,b.height());Bitmap high=Bitmap.createBitmap(w*4,h*4,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(high);c.scale(w*4/64f,h*4/64f);render(c);image=Bitmap.createScaledBitmap(high,w,h,true);if(image!=high)high.recycle();CACHE.put(key,image);}canvas.drawBitmap(image,null,b,paint);}
 static void render(Canvas c){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DITHER_FLAG);p.setShader(new LinearGradient(0,0,64,64,0xff1c3043,0xff0a1625,Shader.TileMode.CLAMP));c.drawCircle(32,32,31,p);p.setShader(null);p.setColor(0xff62dfbd);c.drawCircle(32,32,21,p);p.setColor(0xff102535);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(11,32,53,32,p);c.drawLine(32,11,32,53,p);Path path=new Path();path.moveTo(19,15.5f);path.cubicTo(31,24,31,40,19,48.5f);c.drawPath(path,p);path.reset();path.moveTo(45,15.5f);path.cubicTo(33,24,33,40,45,48.5f);c.drawPath(path,p);p.setStrokeWidth(1.6f);c.drawCircle(32,32,21,p);}
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
