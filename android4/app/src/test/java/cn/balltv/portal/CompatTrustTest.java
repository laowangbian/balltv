package cn.balltv.portal;
import java.io.*;import java.security.cert.*;import org.junit.Test;import static org.junit.Assert.*;
public class CompatTrustTest {
 CompatTrust trust()throws Exception{try(InputStream in=new FileInputStream("src/main/assets/compat-roots.pem")){return CompatTrust.load(in);}}
 X509Certificate cert(String name)throws Exception{try(InputStream in=getClass().getResourceAsStream("/"+name)){return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);}}
 @Test public void documentedRootBundleIsLoadableAndValidated()throws Exception{assertTrue(trust().getAcceptedIssuers().length>=6);}
 @Test public void unknownSelfSignedCertificateIsRejected()throws Exception{try{trust().checkServerTrusted(new X509Certificate[]{cert("untrusted-root.pem")},"RSA");fail();}catch(CertificateException expected){}}
 @Test public void expiredCertificateIsRejected()throws Exception{try{trust().checkServerTrusted(new X509Certificate[]{cert("expired-root.pem")},"RSA");fail();}catch(CertificateExpiredException expected){}}
 @Test public void emptyChainIsRejected()throws Exception{try{trust().checkServerTrusted(new X509Certificate[0],"RSA");fail();}catch(CertificateException expected){}}
 @Test public void incompleteOrModifiedRootBundleIsNotSilentlyAccepted()throws Exception{try(InputStream in=getClass().getResourceAsStream("/untrusted-root.pem")){try{CompatTrust.load(in);fail();}catch(CertificateException expected){}}}
}
