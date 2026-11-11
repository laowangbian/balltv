package cn.balltv.portal;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.util.regex.*;import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
public class NeuralModelTest {
 String read(String p)throws Exception{return new String(Files.readAllBytes(new File(p).toPath()),StandardCharsets.UTF_8);}
 @Test public void realLifeWeightsAndPositiveNegativeActivationsSurviveGlesExport()throws Exception{
  JSONObject weights=new JSONObject(read("../third-party/websr/cnn-2x-s-rl.json")).getJSONObject("layers");JSONArray passes=new JSONObject(read("src/main/assets/neural/model.json")).getJSONArray("passes");String[] names={"conv2d_tf","conv2d_1_tf","conv2d_2_tf","conv2d_last_tf"};assertEquals(4,passes.length());
  for(int i=0;i<4;i++){String shader=read("src/main/assets/neural/"+passes.getJSONObject(i).getString("file"));JSONArray expected=weights.getJSONObject(names[i]).getJSONArray("weights");Matcher matrices=Pattern.compile("mat4\\(([^)]+)\\)").matcher(shader);int matrix=0;while(matrices.find()){int slot=i==0?matrix:matrix/2+(matrix%2)*9;int at=slot*16;for(String number:matrices.group(1).split(","))assertEquals(expected.getDouble(at++),Double.parseDouble(number),1e-9);matrix++;}assertEquals(expected.length(),matrix*16);assertEquals(i>0,shader.contains("max(-p"));assertTrue(shader.contains("MIT license"));}
 }
}
