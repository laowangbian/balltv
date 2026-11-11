package cn.balltv.portal;
import android.content.Context;import android.graphics.*;import android.widget.ImageView;
/** Rounded image clipping also works on API 19; no outline or blur APIs. */
final class RoundedImageView extends ImageView {
 final Path clip=new Path();final RectF rect=new RectF();
 RoundedImageView(Context c){super(c);setScaleType(ScaleType.CENTER_CROP);}
 protected void onSizeChanged(int w,int h,int oldW,int oldH){super.onSizeChanged(w,h,oldW,oldH);rect.set(0,0,w,h);clip.reset();float radius=12*getResources().getDisplayMetrics().density;clip.addRoundRect(rect,radius,radius,Path.Direction.CW);}
 public void draw(Canvas c){int save=c.save();c.clipPath(clip);super.draw(c);c.restoreToCount(save);}
}
