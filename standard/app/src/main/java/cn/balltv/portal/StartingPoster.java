package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;
final class StartingPoster extends Drawable {
 final Paint p=new Paint(3);
 public void draw(Canvas canvas){Rect r=getBounds();p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,Ui.LIGHT?0xffe5edf9:0xff22334e,Ui.LIGHT?0xfff1f5fc:0xff142137,Shader.TileMode.CLAMP));canvas.drawRect(r,p);p.setShader(null);float x=r.exactCenterX(),y=r.top+r.height()*.37f,rad=r.height()*.16f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1,r.height()*.012f));p.setColor(Ui.GREEN);canvas.drawCircle(x,y,rad,p);canvas.drawLine(x,y,x,y-rad*.55f,p);canvas.drawLine(x,y,x+rad*.45f,y+rad*.2f,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(r.height()*.13f);canvas.drawText("赛事预告",x,r.top+r.height()*.78f,p);}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
