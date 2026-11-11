package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import java.net.*;import javax.net.ssl.*;import java.security.cert.*;import java.util.zip.*;
public class TransportCompatTest {
 final String json="{\"name\":\"球场 TV\",\"code\":200}";
 String read(byte[] bytes,String encoding)throws Exception{return Feeds.read(HttpData.decoded(new ByteArrayInputStream(bytes),encoding),10000);}
 @Test public void gzipJsonWorksWithAndWithoutEncodingHeader()throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(out)){zip.write(json.getBytes("UTF-8"));}assertEquals(json,read(out.toByteArray(),"gzip"));assertEquals(json,read(out.toByteArray(),null));}
 @Test public void alreadyDecodedAndroidResponseIsNotDecodedAgain()throws Exception{assertEquals(json,read(json.getBytes("UTF-8"),"gzip"));}
 @Test public void deflateSupportsWrappedAndRawVariants()throws Exception{for(boolean raw:new boolean[]{false,true}){ByteArrayOutputStream out=new ByteArrayOutputStream();try(DeflaterOutputStream zip=new DeflaterOutputStream(out,new Deflater(6,raw))){zip.write(json.getBytes("UTF-8"));}assertEquals(json,read(out.toByteArray(),"deflate"));}}
 @Test public void unsupportedCompressionIsAnExplicitFailure()throws Exception{try{read(json.getBytes("UTF-8"),"br");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("压缩"));}}
 @Test public void certificateCauseIsReportedInsideHandshakeWrapper(){SSLHandshakeException e=new SSLHandshakeException("handshake");e.initCause(new CertificateException("bad certificate"));assertTrue(NetworkIssue.describe(e).contains("证书"));}
 @Test public void groupedDnsFailureIsNotLost(){assertTrue(NetworkIssue.describe(new IOException("group failed",new UnknownHostException("private.example"))).contains("DNS"));assertFalse(NetworkIssue.describe(new IOException("group failed",new UnknownHostException("private.example"))).contains("private.example"));}
 @Test public void timeoutsAndForbiddenAreDistinct(){assertTrue(NetworkIssue.describe(new SocketTimeoutException()).contains("超时"));assertTrue(NetworkIssue.describe(new IOException("HTTP 403")).contains("403"));assertTrue(NetworkIssue.describe(new SSLHandshakeException("bad protocol")).contains("TLS"));}
}
