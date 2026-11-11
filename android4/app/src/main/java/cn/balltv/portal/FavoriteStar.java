package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Font-independent outlined/filled favorite icon, including older television fonts. */
final class FavoriteStar extends Drawable {
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final boolean saved;final int size;
 FavoriteStar(boolean saved,int size){this.saved=saved;this.size=size;setBounds(0,0,size,size);}
 public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
 public void draw(Canvas canvas){Rect b=getBounds();float radius=Math.min(b.width(),b.height())*.43f;Path path=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?radius:radius*.45f;float x=b.exactCenterX()+(float)Math.cos(angle)*r,y=b.exactCenterY()+(float)Math.sin(angle)*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();paint.setColor(saved?0xffffc96b:Ui.TEXT);paint.setStyle(saved?Paint.Style.FILL:Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,size*.075f));paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(path,paint);}
 public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
