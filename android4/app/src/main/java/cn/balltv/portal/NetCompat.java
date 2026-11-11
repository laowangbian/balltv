package cn.balltv.portal;
import java.io.*;import java.net.*;import javax.net.ssl.*;
/** Enables supported TLS 1.2 on KitKat without bypassing certificate validation. */
public final class NetCompat extends SSLSocketFactory {
    final SSLSocketFactory delegate;
    NetCompat(SSLSocketFactory d){delegate=d;}
    static boolean installed;static String engine="系统 TLS",detail="";
    static synchronized void install(android.content.Context owner){if(installed)return;installed=true;try{if(android.os.Build.VERSION.SDK_INT<29){java.security.Provider provider=null;try{if(org.conscrypt.Conscrypt.isAvailable())provider=org.conscrypt.Conscrypt.newProvider();}catch(Throwable unsupported){detail="兼容引擎不可用，使用系统 TLS";}SSLContext context=provider==null?SSLContext.getInstance("TLS"):SSLContext.getInstance("TLS",provider);CompatTrust trust;try(InputStream roots=owner.getAssets().open("compat-roots.pem")){trust=CompatTrust.load(roots);}context.init(null,new TrustManager[]{trust},null);HttpsURLConnection.setDefaultSSLSocketFactory(new NetCompat(context.getSocketFactory()));engine=provider==null?"系统 TLS 1.2 + 补充根证书":"Conscrypt TLS + 补充根证书";}}catch(Exception failure){detail=NetworkIssue.describe(failure);try{SSLContext context=SSLContext.getInstance("TLS");context.init(null,null,null);HttpsURLConnection.setDefaultSSLSocketFactory(new NetCompat(context.getSocketFactory()));}catch(Exception ignored){}}}
    Socket tls(Socket s){if(s instanceof SSLSocket){SSLSocket ssl=(SSLSocket)s;java.util.ArrayList<String> protocols=new java.util.ArrayList<String>();for(String p:ssl.getSupportedProtocols())if(p.equals("TLSv1.2")||p.equals("TLSv1.3"))protocols.add(p);if(!protocols.isEmpty())ssl.setEnabledProtocols(protocols.toArray(new String[0]));}return s;}
    public String[] getDefaultCipherSuites(){return delegate.getDefaultCipherSuites();}public String[] getSupportedCipherSuites(){return delegate.getSupportedCipherSuites();}
    public Socket createSocket()throws IOException{return tls(delegate.createSocket());}
    public Socket createSocket(Socket s,String h,int p,boolean close)throws IOException{return tls(delegate.createSocket(s,h,p,close));}
    public Socket createSocket(String h,int p)throws IOException{return tls(delegate.createSocket(h,p));}
    public Socket createSocket(String h,int p,InetAddress l,int lp)throws IOException{return tls(delegate.createSocket(h,p,l,lp));}
    public Socket createSocket(InetAddress h,int p)throws IOException{return tls(delegate.createSocket(h,p));}
    public Socket createSocket(InetAddress h,int p,InetAddress l,int lp)throws IOException{return tls(delegate.createSocket(h,p,l,lp));}
}

