package cn.balltv.portal;
import android.app.*;
import android.os.Build;
import android.content.Context;
import android.widget.*;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

final class VideoSettings {
 static String mode(Context c){return c.getSharedPreferences("video",0).getString("mode","auto");}
 static boolean eligible(Context c){ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);return am!=null&&VideoPolicy.enhancementEligible(Build.VERSION.SDK_INT,am.isLowRamDevice(),am.getMemoryClass(),am.getDeviceConfigurationInfo().reqGlEsVersion);}
 static boolean enhance(Context c){return eligible(c)&&c.getSharedPreferences("video",0).getBoolean("enhance",false);}
 static void configure(Context c,ExoPlayer player){
  String mode=mode(c);int h=VideoPolicy.maxHeight(mode);
  DefaultTrackSelector selector=(DefaultTrackSelector)player.getTrackSelector();
  DefaultTrackSelector.Parameters.Builder builder=selector.buildUponParameters()
   .setMaxVideoSize(h*16/9,h).setMaxVideoFrameRate(Ui.LEAN?30:60)
   .setMaxVideoBitrate(VideoPolicy.maxBitrate(mode,Ui.LEAN))
   .setForceHighestSupportedBitrate(false)
   .setExceedRendererCapabilitiesIfNecessary(false)
   .setExceedVideoConstraintsIfNecessary(true);
  if(Ui.LEAN)builder.setPreferredVideoMimeTypes("video/avc");
  selector.setParameters(builder);
 }
 static boolean requested(Context c,boolean ai){return c.getSharedPreferences("video",0).getBoolean(ai?"ai":"enhance",false);}
 static String state(Activity a,boolean ai){if(!requested(a,ai))return "已关闭";if(!eligible(a))return "已开启 · 设备不支持";if(a instanceof PlayerActivity&&((PlayerActivity)a).enhancementBlocked)return "已开启 · 本次已回退";return "已开启";}
 static void show(Activity a,Runnable changed){
  String current=mode(a);
  Ui.options(a,"画质与增强",new String[]{"原始信号 · "+(current.equals("smooth")?"流畅优先":current.equals("high")?"高清优先":"自动平衡"),"轻量 GPU 锐化 · "+state(a,false),"增强说明与设备支持"},index->{
   if(index==0)Ui.selectedOptions(a,"原始信号画质",new String[]{"自动平衡 · 推荐","流畅优先 · 最高目标 720p","高清优先 · 最高目标 1080p"},current.equals("smooth")?1:current.equals("high")?2:0,i->{a.getSharedPreferences("video",0).edit().putString("mode",new String[]{"auto","smooth","high"}[i]).apply();if(changed!=null)changed.run();});
   else if(index==1){
    if(!eligible(a)){Ui.message(a,"当前设备保持原始画面","轻量 GPU 锐化需 Android 8.0+、OpenGL ES 3.0、非低内存设备及至少 192 MB 应用内存等级。当前设备不满足条件，增强保持关闭。仍可调整原始信号画质。");return;}
    Ui.selectedOptions(a,"轻量 GPU 锐化 · 当前设置："+state(a,false),new String[]{"关闭 · 原始画面","开启 · 实验性轻度锐化"},requested(a,false)?1:0,i->{a.getSharedPreferences("video",0).edit().putBoolean("enhance",i==1).apply();if(changed!=null)changed.run();});
   }else Ui.message(a,"画质增强说明","优先使用可用高清线路，并按带宽与解码能力自适应。目标分辨率仅适用于有多档画质的信号；单档信号保持可播放，无法凭空增加细节。\n\nGPU 锐化是轻量画面后处理，不是 AI 超分，不增加视频源的真实分辨率。默认关闭，旧系统不启用；开启会增加 GPU 占用，持续掉帧或处理异常时本次播放会自动恢复原始画面。HDR 保持原始细节，不额外锐化。\n\n当前设备："+(eligible(a)?"可尝试 GPU 锐化，实际流畅性仍需设备验证":"保持原始画面")+"。无需登录，无模型下载，无云端视频上传。");
  });
 }
}
