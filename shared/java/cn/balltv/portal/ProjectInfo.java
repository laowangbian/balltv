package cn.balltv.portal;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;

final class ProjectInfo {
    static final String REPOSITORY = "https://github.com/laowangbian/balltv";
    static final String QUARK = "https://pan.quark.cn/s/4353e196c729";

    static void about(Activity activity) {
        LinearLayout content = Ui.column(activity);
        addText(activity, content, "作者 · 老网编");
        link(activity, content, "GitHub 项目", REPOSITORY);
        link(activity, content, "微博 · 老网编自留地", "https://weibo.com/u/1985414793");
        link(activity, content, "知乎 · 老网编", "https://www.zhihu.com/people/baigh");
        content.addView(Ui.button(activity, "微信与交流分享群", view -> contact(activity)));
        Ui.dialog(activity, "关于与交流", content);
    }

    static void contact(Activity activity) {
        showCode(activity, "微信与交流分享群", "扫码加老网编微信，备注「球场 TV」可申请加入交流分享群。扫码加不上时，可搜索微信号 wangbianlaole。", "wechat-contact.png", new Rect(120, 405, 852, 1137));
    }

    static void donation(Activity activity) {
        showCode(activity, "赞赏支持", "感谢支持球场 TV。可用微信扫码自愿赞赏。", "wechat-donation.jpg", null);
    }

    private static void showCode(Activity activity, String title, String description, String name, Rect region) {
        LinearLayout content = Ui.column(activity);
        try (InputStream in = activity.getAssets().open(name)) {
            Bitmap image;
            if (region == null) {
                image = BitmapFactory.decodeStream(in);
            } else {
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, false);
                try { image = decoder.decodeRegion(region, new BitmapFactory.Options()); }
                finally { decoder.recycle(); }
            }
            if (image == null) throw new IOException("QR image unavailable");
            ImageView code = new ImageView(activity);
            code.setContentDescription(title + "二维码");
            code.setScaleType(ImageView.ScaleType.FIT_CENTER);
            code.setBackgroundColor(Color.WHITE);
            code.setImageBitmap(image);
            int size = Math.min(Ui.dp(activity, 330), (int)(activity.getResources().getDisplayMetrics().heightPixels * .42f));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(size, size);
            layout.gravity = Gravity.CENTER_HORIZONTAL;
            content.addView(code, layout);
        } catch (IOException | IllegalArgumentException error) {
            addText(activity, content, "二维码暂时无法显示。");
        }
        addText(activity, content, description);
        Ui.dialog(activity, title, content);
    }

    static void addText(Activity activity, LinearLayout content, String message) {
        TextView text = Ui.text(activity, message, 16, Ui.TEXT);
        text.setPadding(0, Ui.dp(activity, 10), 0, Ui.dp(activity, 10));
        content.addView(text);
    }

    static void link(Activity activity, LinearLayout content, String label, String url) {
        content.addView(Ui.button(activity, label, view -> {
            try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (ActivityNotFoundException error) { Ui.message(activity, label, url); }
        }));
    }
}
