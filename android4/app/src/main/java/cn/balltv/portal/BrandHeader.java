package cn.balltv.portal;
import android.content.Context;import android.graphics.Typeface;import android.view.Gravity;import android.widget.*;
/** Compact aligned lockup; name and subtitle share the same left edge. */
final class BrandHeader extends LinearLayout {
 BrandHeader(Context c){super(c);setGravity(Gravity.CENTER_VERTICAL);setPadding(Ui.dp(c,4),0,0,Ui.dp(c,25));ImageView mark=new ImageView(c);mark.setImageDrawable(new BrandMark());addView(mark,new LayoutParams(Ui.dp(c,36),Ui.dp(c,36)));LinearLayout type=Ui.column(c);type.setPadding(Ui.dp(c,8),0,0,0);TextView name=Ui.text(c,"球场 TV",19,Ui.TEXT);name.setSingleLine();name.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));type.addView(name);TextView subtitle=Ui.text(c,"LIVE / MATCHDAY",8,Ui.MUTED);subtitle.setSingleLine();subtitle.setPadding(0,Ui.dp(c,5),0,0);type.addView(subtitle);addView(type,new LayoutParams(0,-2,1));}
}
