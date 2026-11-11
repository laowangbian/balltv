package cn.balltv.portal;
import android.app.*;
import android.os.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
public class VideoActivity extends Activity {
    VideoView video;int position; boolean playing=true;
    public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(128);String url=getIntent().getStringExtra("url");if(!Ui.webUrl(url)){finish();return;}LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);setContentView(root);LinearLayout bar=new LinearLayout(this);root.addView(bar);bar.addView(Ui.button(this,"返回",v->finish()));bar.addView(Ui.button(this,"播放/暂停",v->{if(video.isPlaying())video.pause();else video.start();}));bar.addView(Ui.button(this,"外部播放器",v->Ui.external(this,url,true)));root.addView(Ui.text(this,"视频直链 · "+getIntent().getStringExtra("name"),17,Ui.MUTED));video=new VideoView(this);root.addView(video,new LinearLayout.LayoutParams(-1,0,1));MediaController controls=new MediaController(this);controls.setAnchorView(video);video.setMediaController(controls);video.setOnPreparedListener(mp->{if(position>0)video.seekTo(position);if(playing)video.start();});video.setOnErrorListener((mp,what,extra)->{Ui.message(this,"播放失败","系统播放器不支持此地址或格式（"+what+" / "+extra+"）。可换地址或外部播放器。网页地址应以网页入口添加。");return true;});video.setVideoURI(Uri.parse(url));video.requestFocus();}
    protected void onPause(){playing=video.isPlaying();position=video.getCurrentPosition();video.pause();super.onPause();}protected void onResume(){super.onResume();if(video!=null&&playing)video.start();}protected void onDestroy(){video.stopPlayback();super.onDestroy();}
}

