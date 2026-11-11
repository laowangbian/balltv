package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;
final class CoverPlaceholder extends Drawable {
 final Paint paint=new Paint(3);
 public void draw(Canvas c){Rect r=getBounds();paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,Ui.LIGHT?0xffe0e9f6:0xff1c2c46,Ui.LIGHT?0xffedf2fa:0xff131d30,Shader.TileMode.CLAMP));c.drawRect(r,paint);paint.setShader(null);paint.setColor(Ui.LIGHT?0xffbacce5:0xff344965);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);float x=r.left+r.width()*.16f,y=r.top+r.height()*.15f,w=r.width()*.68f,h=r.height()*.49f;c.drawRoundRect(new RectF(x,y,x+w,y+h),6,6,paint);c.drawLine(x+w/2,y,x+w/2,y+h,paint);c.drawCircle(x+w/2,y+h/2,h*.22f,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(r.height()*.1f);c.drawText("等待比赛画面",r.exactCenterX(),r.top+r.height()*.85f,paint);}
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
