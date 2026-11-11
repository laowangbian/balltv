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
 static boolean aiEligible(Context c){ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);if(!eligible(c)||am==null)return false;ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();am.getMemoryInfo(memory);return memory.totalMem>=1900000000L;}
 static boolean ai(Context c){return aiEligible(c)&&c.getSharedPreferences("video",0).getBoolean("ai",false);}
 static boolean sharpen(Context c){return eligible(c)&&c.getSharedPreferences("video",0).getBoolean("enhance",false)&&!ai(c);}
 static boolean enhance(Context c){return sharpen(c)||ai(c);}
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
  Ui.options(a,"画质与增强"+(a instanceof PlayerActivity&&!((PlayerActivity)a).aiState.isEmpty()?" · "+((PlayerActivity)a).aiState:""),new String[]{"原始信号 · "+(current.equals("smooth")?"流畅优先":current.equals("high")?"高清优先":"自动平衡"),"轻量 GPU 锐化 · "+state(a,false),"增强说明与设备支持","AI 画质增强 · "+state(a,true),"AI 模型与开源许可"},index->{
   if(index==3){if(!aiEligible(a)){Ui.message(a,"当前设备保持原始画面","AI 超分要求 Android 5.0+、OpenGL ES 3.0、非低内存设备、至少约 2GB 内存及 192MB 应用内存等级。满足条件也不保证实时，开启后会按实际输入和处理耗时保护回退。");return;}Ui.selectedOptions(a,"AI 画质增强 · 当前设置："+state(a,true),new String[]{"关闭 AI 画质增强","开启 · 实验性神经网络增强"},requested(a,true)?1:0,i->{a.getSharedPreferences("video",0).edit().putBoolean("ai",i==1).putBoolean("enhance",false).apply();if(changed!=null)changed.run();});return;}
   if(index==4){try{Ui.message(a,"AI 模型与开源许可","RealLife CNN S 2×，WebSR real-life 训练权重 · Copyright 2023 Sam Bhattacharyya。WebSR 权重和导出着色器遵循 MIT，Android GPU 运行适配器保留 LGPL-3.0-or-later；包含原始模型、改编源文件和许可证，可随源码重新构建替换。无担保。\n\n"+NeuralEffect.read(a,"neural/NOTICE.txt"));}catch(Exception error){Ui.message(a,"模型许可","源码包 third-party/websr 内包含原始着色器和许可证。");}return;}
   if(index==0)Ui.selectedOptions(a,"原始信号画质",new String[]{"自动平衡 · 推荐","流畅优先 · 最高目标 720p","高清优先 · 最高目标 1080p"},current.equals("smooth")?1:current.equals("high")?2:0,i->{a.getSharedPreferences("video",0).edit().putString("mode",new String[]{"auto","smooth","high"}[i]).apply();if(changed!=null)changed.run();});
   else if(index==1){
    if(!eligible(a)){Ui.message(a,"当前设备保持原始画面","轻量 GPU 锐化需 Android 5.0+、OpenGL ES 3.0、非低内存设备及至少 192 MB 应用内存等级。当前设备不满足条件，增强保持关闭。仍可调整原始信号画质。");return;}
    Ui.selectedOptions(a,"轻量 GPU 锐化 · 当前设置："+state(a,false),new String[]{"关闭 · 原始画面","开启 · 实验性轻度锐化"},requested(a,false)?1:0,i->{a.getSharedPreferences("video",0).edit().putBoolean("enhance",i==1).putBoolean("ai",false).apply();if(changed!=null)changed.run();});
   }else Ui.message(a,"画质增强说明","优先使用可用高清线路，并按带宽与解码能力自适应。目标分辨率仅适用于有多档画质的信号；单档信号保持可播放，无法凭空增加细节。\n\nGPU 锐化是轻量画面后处理，不是 AI 超分，不增加视频源的真实分辨率。默认关闭，旧系统不启用；开启会增加 GPU 占用，持续掉帧或处理异常时本次播放会自动恢复原始画面。HDR 保持原始细节，不额外锐化。\n\nAI 超分另提供 RealLife CNN S 训练模型的 2× 亮度残差重建，默认关闭，支持安全尺寸范围内的 SDR 输入，包括 1440p、4K 编码信号。保留原始纹理，按实际帧间隔和设备负载调整神经网络推理尺寸，再合成细节残差。降低的是 AI 推理成本，原始画面细节不会被整体缩小替换；较低推理档位的增强可能更轻微。启动预热和短时波动不会直接关闭；最低推理负载仍持续过载、纹理不支持或播放异常时才回退。编码尺寸、帧率与实际清晰度不是同一概念，低码率或上游放大造成的细节损失无法保证恢复。效果依信号而异，并非体育专用训练模型。\n\n当前设备："+(eligible(a)?"可尝试 GPU 锐化，实际流畅性仍需设备验证":"保持原始画面")+"。无需登录，无模型下载，无云端视频上传。");
  });
 }
}
