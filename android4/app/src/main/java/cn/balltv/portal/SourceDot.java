package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Stable-size source state indicator, independent of font glyphs. */
final class SourceDot extends Drawable {
 final boolean loading,failed,ready;final int size;final Paint p=new Paint(3);
 SourceDot(boolean l,boolean f,boolean r,int s){loading=l;failed=f;ready=r;size=s;setBounds(0,0,s,s);}
 public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
 public void draw(Canvas c){float x=getBounds().exactCenterX(),y=getBounds().exactCenterY();p.setColor(failed?0xffe77d7d:ready?Ui.GREEN:Ui.MUTED);p.setStyle(loading?Paint.Style.STROKE:Paint.Style.FILL);p.setStrokeWidth(size*.12f);p.setPathEffect(loading?new DashPathEffect(new float[]{size*.13f,size*.15f},0):null);c.drawCircle(x,y,size*(loading?.31f:.30f),p);}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
