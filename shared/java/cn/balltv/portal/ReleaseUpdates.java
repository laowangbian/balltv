package cn.balltv.portal;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

final class ReleaseUpdates {
    private static final String API = "https://api.github.com/repos/laowangbian/balltv/releases/latest";
    private static final long APK_LIMIT = 100L * 1024 * 1024;
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final class UpdateError extends IOException { UpdateError(String message) { super(message); } }

    static void show(Activity activity) {
        LinearLayout content = Ui.column(activity);
        ProjectInfo.addText(activity, content, "当前版本 · " + installed(activity).versionName);
        Dialog dialog = Ui.dialog(activity, "检查更新", content);
        content.addView(Ui.button(activity, "从 GitHub 检查", view -> {
            dialog.dismiss();
            run(activity, "正在检查 GitHub 更新", () -> readRelease(installed(activity).versionName), release -> {
                if (release == null) Ui.message(activity, "已是最新版本", "当前版本已是 GitHub 上的最新正式版。");
                else offer(activity, release);
            });
        }));
        manual(activity, content);
    }

    private static void manual(Activity activity, LinearLayout content) {
        ProjectInfo.addText(activity, content, "GitHub 访问不稳定时，可打开夸克网盘下载对应版本的 APK，手动覆盖安装。\n" + ProjectInfo.QUARK);
        ProjectInfo.link(activity, content, "打开夸克网盘", ProjectInfo.QUARK);
    }

    private static void offer(Activity activity, Release release) {
        LinearLayout content = Ui.column(activity);
        ProjectInfo.addText(activity, content, release.filename + "\n下载完成后，由系统确认安装。");
        Dialog dialog = Ui.dialog(activity, "发现新版本 v" + release.version, content);
        content.addView(Ui.button(activity, "下载并校验", view -> {
            dialog.dismiss();
            run(activity, "正在下载并校验 APK", () -> download(activity, release), file -> installPrompt(activity, file));
        }));
        ProjectInfo.link(activity, content, "查看 GitHub 发布说明", ProjectInfo.REPOSITORY + "/releases/tag/v" + release.version);
        manual(activity, content);
    }

    private static void installPrompt(Activity activity, File file) {
        LinearLayout content = Ui.column(activity);
        ProjectInfo.addText(activity, content, "APK 校验通过。安装后可保留原有设置和收藏。");
        Dialog dialog = Ui.dialog(activity, "安装更新", content);
        content.addView(Ui.button(activity, "打开系统安装器", view -> {
            try {
                if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
                    Ui.message(activity, "允许安装更新", "请在系统设置允许球场 TV 安装应用，返回后再次打开系统安装器。");
                    activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
                    return;
                }
                Uri uri = Build.VERSION.SDK_INT < 24 ? Uri.fromFile(file)
                    : Uri.parse("content://" + activity.getPackageName() + ".updates/" + file.getName());
                Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri("球场 TV 更新", uri));
                activity.startActivity(intent);
                dialog.dismiss();
            } catch (RuntimeException error) {
                failure(activity, "系统安装器无法打开。请检查电视的应用安装设置。");
            }
        }));
        manual(activity, content);
    }

    private static PackageInfo installed(Activity activity) {
        try { return activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_SIGNATURES); }
        catch (PackageManager.NameNotFoundException error) { throw new IllegalStateException(error); }
    }

    static final class Release {
        final String version, filename, url, digest;
        final long size;
        Release(String version, String filename, String url, String digest, long size) {
            this.version=version; this.filename=filename; this.url=url; this.digest=digest; this.size=size;
        }
    }

    static Release select(JSONObject json, String current) throws Exception {
        String tag = json.getString("tag_name");
        if (json.optBoolean("draft") || json.optBoolean("prerelease") || !tag.matches("v[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")) throw new UpdateError("正式版信息无效");
        String version = tag.substring(1);
        boolean legacy = current.endsWith("-android4");
        String base = legacy ? current.substring(0, current.length()-9) : current;
        if (compare(version, base) <= 0) return null;
        String filename = "BallTV-" + version + (legacy ? "-android4.apk" : "-standard.apk");
        String expected = ProjectInfo.REPOSITORY + "/releases/download/" + tag + "/" + filename;
        JSONArray assets = json.getJSONArray("assets");
        for (int i=0; i<assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (!filename.equals(asset.optString("name"))) continue;
            long size = asset.optLong("size");
            String digest = asset.optString("digest").toLowerCase(Locale.ROOT);
            if (!expected.equals(asset.optString("browser_download_url")) || !digest.matches("sha256:[0-9a-f]{64}") || size<=0 || size>APK_LIMIT) throw new UpdateError("安装包下载或校验信息不完整");
            return new Release(version, filename, expected, digest.substring(7), size);
        }
        throw new UpdateError("GitHub 发布缺少对应版本的安装包");
    }

    private static int compare(String left, String right) throws IOException {
        if (!right.matches("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")) throw new UpdateError("当前版本格式无效");
        String[] a=left.split("\\."), b=right.split("\\.");
        for (int i=0;i<3;i++) {
            int result=Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (result!=0) return result;
        }
        return 0;
    }

    private static Release readRelease(String current) throws Exception {
        HttpsURLConnection connection = request(new URL(API));
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (connection.getResponseCode()!=200) throw new UpdateError("GitHub 返回 HTTP " + connection.getResponseCode());
            try (InputStream in=connection.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] bytes=new byte[8192];
                for (int n; (n=in.read(bytes))!=-1;) {
                    if (out.size()+n>1024*1024) throw new UpdateError("GitHub 发布信息过大");
                    out.write(bytes,0,n);
                }
                return select(new JSONObject(out.toString("UTF-8")),current);
            }
        } finally { connection.disconnect(); }
    }

    private static HttpsURLConnection request(URL url) throws IOException {
        HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "BallTV Android updater");
        return connection;
    }

    private static boolean downloadHost(URL url) {
        String host=url.getHost();
        return "https".equals(url.getProtocol()) && url.getUserInfo()==null && (url.getPort()==-1 || url.getPort()==443)
            && ("github.com".equals(host) || "release-assets.githubusercontent.com".equals(host) || "objects.githubusercontent.com".equals(host) || "github-releases.githubusercontent.com".equals(host));
    }

    private static File download(Activity activity, Release release) throws Exception {
        // Older Android installers expect a filesystem path. API 19+ lets the app
        // write to its own external files directory without a storage permission.
        File dir = Build.VERSION.SDK_INT < 24 ? activity.getExternalFilesDir("updates") : new File(activity.getCacheDir(), "updates");
        if (dir == null) throw new UpdateError("系统更新目录不可用，请手动安装");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new UpdateError("无法创建更新缓存");
        File temp = File.createTempFile("download-", ".apk", activity.getCacheDir());
        try {
            URL url = new URL(release.url);
            boolean fetched=false;
            for (int redirects=0; redirects<5; redirects++) {
                if (!downloadHost(url)) throw new UpdateError("下载地址不在 GitHub 发布域名范围内");
                HttpsURLConnection connection=request(url);
                connection.setRequestProperty("Accept-Encoding", "identity");
                try {
                    int code=connection.getResponseCode();
                    if (code==301||code==302||code==303||code==307||code==308) {
                        String location=connection.getHeaderField("Location");
                        if (location==null) throw new UpdateError("GitHub 下载跳转无效");
                        url=new URL(url,location);
                        continue;
                    }
                    if (code!=200) throw new UpdateError("下载返回 HTTP " + code);
                    copyVerified(connection, temp, release);
                    fetched=true;
                    break;
                } finally { connection.disconnect(); }
            }
            if (!fetched) throw new UpdateError("GitHub 下载跳转过多");
            PackageInfo active=installed(activity);
            PackageInfo apk=activity.getPackageManager().getPackageArchiveInfo(temp.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            String expectedVersion=release.version+(active.versionName.endsWith("-android4")?"-android4":"");
            if (apk==null || !active.packageName.equals(apk.packageName) || apk.versionCode<=active.versionCode || !expectedVersion.equals(apk.versionName) || active.signatures==null || active.signatures.length==0 || !Arrays.equals(active.signatures,apk.signatures)) throw new UpdateError("APK 包名、版本或签名校验失败");
            File file=new File(dir,"update-"+release.digest+".apk");
            if (Build.VERSION.SDK_INT < 24) {
                try (InputStream in=new java.io.FileInputStream(temp)) { copyVerified(in,file,release); }
                catch(Exception error){file.delete();throw error;}
            } else if (!temp.renameTo(file)) throw new UpdateError("无法保存已校验的安装包");
            File[] previous=dir.listFiles();
            if (previous!=null) for (File old:previous) if (!file.equals(old) && old.getName().matches("update-[0-9a-f]{64}\\.apk")) old.delete();
            return file;
        } finally { temp.delete(); }
    }

    private static void copyVerified(HttpsURLConnection connection, File temp, Release release) throws Exception {
        try (InputStream in=connection.getInputStream()) { copyVerified(in,temp,release); }
    }

    private static void copyVerified(InputStream in, File temp, Release release) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        long size=0;
        long deadline=android.os.SystemClock.elapsedRealtime()+180000;
        try (FileOutputStream out=new FileOutputStream(temp)) {
            byte[] buffer=new byte[32768];
            for (int n; (n=in.read(buffer))!=-1;) {
                size+=n;
                if (size>release.size || android.os.SystemClock.elapsedRealtime()>deadline) throw new UpdateError("下载超出大小或时间限制");
                digest.update(buffer,0,n);
                out.write(buffer,0,n);
            }
        }
        StringBuilder hex=new StringBuilder(64);
        for (byte value:digest.digest()) hex.append(Character.forDigit((value>>4)&15,16)).append(Character.forDigit(value&15,16));
        if (size!=release.size || !release.digest.contentEquals(hex)) throw new UpdateError("下载不完整或 SHA-256 校验失败");
    }

    interface Work<T> {T execute() throws Exception;}
    interface Success<T> {void show(T value);}
    private static <T> void run(Activity activity, String label, Work<T> work, Success<T> success) {
        if (!BUSY.compareAndSet(false,true)) {Ui.message(activity,"更新正在进行","请等待当前检查或下载完成。");return;}
        Dialog progress=Ui.dialog(activity,label,Ui.text(activity,"请稍候。",16,Ui.TEXT));
        Thread thread=new Thread(() -> {
            T result=null;
            String error=null;
            try { result=work.execute(); }
            catch (Exception failure) { error=failure instanceof UpdateError ? failure.getMessage() : "暂时无法完成 GitHub 更新。"; }
            finally { BUSY.set(false); }
            final T value=result;
            final String message=error;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing()||activity.isDestroyed()) return;
                progress.dismiss();
                if (message==null) success.show(value);
                else failure(activity,message);
            });
        },"BallTV-update");
        thread.start();
    }

    private static void failure(Activity activity, String error) {
        LinearLayout content=Ui.column(activity);
        ProjectInfo.addText(activity,content,error);
        manual(activity,content);
        Ui.dialog(activity,"更新未完成",content);
    }
}
