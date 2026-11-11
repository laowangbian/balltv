package cn.balltv.portal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseUpdatesTest {
    private static JSONObject asset(String variant) throws Exception {
        String name="BallTV-0.15.10-"+variant+".apk";
        return new JSONObject().put("name",name).put("size",1024)
            .put("digest","sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
            .put("browser_download_url",ProjectInfo.REPOSITORY+"/releases/download/v0.15.10/"+name);
    }

    private static JSONObject release(JSONObject... assets) throws Exception {
        JSONArray values=new JSONArray();
        for(JSONObject asset:assets) values.put(asset);
        return new JSONObject().put("tag_name","v0.15.10").put("assets",values);
    }

    @Test public void picksTheInstalledVariantAndComparesVersionsNumerically() throws Exception {
        JSONObject json=release(asset("standard"),asset("android4"));
        assertEquals("BallTV-0.15.10-standard.apk",ReleaseUpdates.select(json,"0.15.5").filename);
        assertEquals("BallTV-0.15.10-android4.apk",ReleaseUpdates.select(json,"0.15.9-android4").filename);
        assertNull(ReleaseUpdates.select(json,"0.15.10"));
        assertNull(ReleaseUpdates.select(json,"0.16.0-android4"));
    }

    @Test(expected=java.io.IOException.class)
    public void absentLegacyAssetCannotFallThroughToStandard() throws Exception {
        ReleaseUpdates.select(release(asset("standard")),"0.15.5-android4");
    }

    @Test(expected=java.io.IOException.class)
    public void anAssetFromAnotherProjectCannotBeInstalled() throws Exception {
        ReleaseUpdates.select(release(asset("standard").put("browser_download_url","https://github.com/another/project/releases/download/v0.15.10/BallTV-0.15.10-standard.apk")),"0.15.5");
    }

    @Test(expected=java.io.IOException.class)
    public void refusesAnAssetWithoutASha256Digest() throws Exception {
        ReleaseUpdates.select(release(asset("standard").put("digest","")),"0.15.5");
    }
}
