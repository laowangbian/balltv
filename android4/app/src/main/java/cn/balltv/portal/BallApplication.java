package cn.balltv.portal;
public class BallApplication extends androidx.multidex.MultiDexApplication {
 @Override public void onCreate(){super.onCreate();NetCompat.install(this);}
}
