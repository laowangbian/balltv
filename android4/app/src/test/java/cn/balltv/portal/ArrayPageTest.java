package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import org.json.*;
public class ArrayPageTest {
    @Test public void stopsReadingAfterRequestedPageAndHandlesEscapedBraces()throws Exception{
        String body="[{\"id\":1,\"text\":\"a \\\" } [ b\",\"nested\":{\"list\":[1,2]}},{\"id\":2},{\"id\":3},broken tail";
        StringReader reader=new StringReader(body);JSONArray page=HttpData.readArrayPage(reader,1,2,Long.MAX_VALUE);
        assertEquals(2,page.length());assertEquals(2,page.getJSONObject(0).getInt("id"));assertEquals(3,page.getJSONObject(1).getInt("id"));assertEquals(',',reader.read());
    }
    @Test public void finalPartialAndEmptyPagesAreValid()throws Exception{
        assertEquals(1,HttpData.readArrayPage(new StringReader("[{\"id\":1}]"),0,25,Long.MAX_VALUE).length());
        assertEquals(0,HttpData.readArrayPage(new StringReader("[{\"id\":1}]"),1,25,Long.MAX_VALUE).length());
    }
    @Test(expected=IOException.class) public void rejectsTruncatedObject()throws Exception{HttpData.readArrayPage(new StringReader("[{\"id\":1"),0,25,Long.MAX_VALUE);}
}
