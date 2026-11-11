package cn.balltv.portal;
import android.graphics.*;import android.graphics.drawable.Drawable;
final class NavGlyph extends Drawable {
 final String kind;final Paint p=new Paint(3);final int size;
 NavGlyph(String kind,int color,int size){this.kind=kind;this.size=size;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
 public void draw(Canvas canvas){Rect b=getBounds();int save=canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);if(kind.equals("live")){canvas.drawCircle(12,12,8,p);canvas.drawCircle(12,12,3,p);}else if(kind.equals("schedule")){canvas.drawRoundRect(new RectF(4,5,20,21),3,3,p);canvas.drawLine(4,10,20,10,p);canvas.drawLine(8,3,8,7,p);canvas.drawLine(16,3,16,7,p);canvas.drawLine(8,14,10,14,p);canvas.drawLine(14,14,16,14,p);canvas.drawLine(8,18,10,18,p);}else if(kind.equals("replay")){canvas.drawRoundRect(new RectF(3,5,21,20),3,3,p);Path play=new Path();play.moveTo(10,9);play.lineTo(16,12.5f);play.lineTo(10,16);play.close();canvas.drawPath(play,p);}else if(kind.equals("favorites")){Path star=new Path();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5;float r=i%2==0?9:4.3f,x=12+(float)Math.cos(angle)*r,y=12+(float)Math.sin(angle)*r;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();canvas.drawPath(star,p);}else{canvas.drawCircle(12,12,8,p);canvas.drawLine(12,7,12,12,p);canvas.drawLine(12,12,16,14,p);}canvas.restoreToCount(save);}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
