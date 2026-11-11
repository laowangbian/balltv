// BallTV RealLife CNN S adapter, modified 2026-11-10. LGPL-3.0-or-later.
package cn.balltv.portal;

import android.content.Context;
import android.opengl.GLES20;
import com.google.android.exoplayer2.effect.*;
import com.google.android.exoplayer2.util.*;
import java.util.*;
import java.io.*;
import org.json.*;

/** Executes the separate LGPL RealLife CNN S shader model without CPU video copies. */
final class NeuralEffect implements GlEffect {
 interface Status {void report(String text,boolean stop);}
 volatile float declaredFps;volatile long dropsAt;volatile int inferenceHeight;
 void nominal(float fps){declaredFps=fps>0?fps:0;}
 void dropped(int count,long elapsed){if(count>=12&&elapsed>=1000&&count*1000L/elapsed>=8)dropsAt=android.os.SystemClock.elapsedRealtime();}
 String tierLabel(){return inferenceHeight>0?inferenceHeight+"p推理":"自适应";}
 final Status status;
 NeuralEffect(Status status){this.status=status;}
 @Override public GlShaderProgram toGlShaderProgram(Context c,boolean hdr)throws VideoFrameProcessingException{return new Program(c,hdr,status,this);}
 static String read(Context c,String file)throws IOException{try(InputStream in=c.getAssets().open(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}}
 static final String VERTEX="attribute vec4 aPosition; varying vec2 vTex; void main(){gl_Position=aPosition;vTex=aPosition.xy*0.5+0.5;}";
 static final String COLOR="vec3 toGamma(vec3 x){return mix(4.5*x,1.099*pow(max(x,vec3(0.)),vec3(.45))-.099,step(vec3(.018),x));}vec3 toLinear(vec3 x){return mix(x/4.5,pow(max((x+.099)/1.099,vec3(0.)),vec3(1./.45)),step(vec3(.081),x));}";
 static final class Target {int texture,fbo;Target(int texture,int fbo){this.texture=texture;this.fbo=fbo;}}
 static final class Pass {GlProgram program;String save;String[] binds;}
 static final class Program extends BaseGlShaderProgram {
  final int displayWidth,displayHeight;final Status status;final boolean hdr;final ArrayList<Pass> passes=new ArrayList<Pass>();final Map<String,Target> targets=new LinkedHashMap<String,Target>();
  GlProgram extract,combine,identity;int width,height,sourceWidth,sourceHeight,outWidth,outHeight,frames,tier;int[][] levels;boolean bypass,stopReported;final NeuralEffect control;final FrameCadence cadence=new FrameCadence();final NeuralPolicy.Monitor monitor=new NeuralPolicy.Monitor();GpuTimer gpuTimer;
  Program(Context c,boolean hdr,Status status,NeuralEffect control)throws VideoFrameProcessingException{
   super(hdr,1);displayWidth=c.getResources().getDisplayMetrics().widthPixels;displayHeight=c.getResources().getDisplayMetrics().heightPixels;this.hdr=hdr;this.status=status;this.control=control;
   try{
    identity=program("precision highp float;varying vec2 vTex;uniform sampler2D uTexture;void main(){gl_FragColor=texture2D(uTexture,vTex);}");
    if(hdr)return;
    extract=program("precision highp float;varying vec2 vTex;uniform sampler2D uTexture;"+COLOR+"void main(){vec3 rgb=toGamma(texture2D(uTexture,vTex).rgb);gl_FragColor=vec4(rgb,1.);}");
    JSONArray list=new JSONObject(read(c,"neural/model.json")).getJSONArray("passes");
    for(int i=0;i<list.length();i++){JSONObject row=list.getJSONObject(i);Pass pass=new Pass();pass.program=program(read(c,"neural/"+row.getString("file")));pass.save=row.getString("save");JSONArray names=row.getJSONArray("binds");pass.binds=new String[names.length()];for(int n=0;n<names.length();n++)pass.binds[n]=names.getString(n);passes.add(pass);}
    combine=program("precision highp float;varying vec2 vTex;uniform sampler2D uOriginal;uniform sampler2D uPredicted;uniform vec2 uSize;uniform float uGain;"+COLOR+"float prediction(vec2 pixel){pixel=clamp(pixel,vec2(0.),uSize*2.-1.);vec2 phase=mod(pixel,2.);vec4 p=texture2D(uPredicted,(floor(pixel*.5)+.5)/uSize);return phase.x<.5?(phase.y<.5?p.r:p.b):(phase.y<.5?p.g:p.a);}void main(){vec2 grid=vTex*uSize*2.-.5;vec2 cell=floor(grid);vec2 f=fract(grid);float y=mix(mix(prediction(cell),prediction(cell+vec2(1.,0.)),f.x),mix(prediction(cell+vec2(0.,1.)),prediction(cell+vec2(1.,1.)),f.x),f.y);vec4 original=texture2D(uOriginal,vTex);vec3 rgb=toGamma(original.rgb);gl_FragColor=vec4(toLinear(clamp(rgb+vec3(clamp(y,-.08,.08)*uGain),0.,1.)),original.a);}");
   }catch(Exception error){try{release();}catch(Exception ignored){}throw new VideoFrameProcessingException(error);}
  }
  GlProgram program(String fragment)throws GlUtil.GlException{GlProgram p=new GlProgram(VERTEX,fragment);p.setBufferAttribute("aPosition",GlUtil.getNormalizedCoordinateBounds(),GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);return p;}
  @Override public Size configure(int w,int h)throws VideoFrameProcessingException{
   try{
    if(w==sourceWidth&&h==sourceHeight&&(bypass||!targets.isEmpty()))return new Size(bypass?w:outWidth,bypass?h:outHeight);
    clearTargets();sourceWidth=w;sourceHeight=h;frames=0;stopReported=false;cadence.reset();bypass=!NeuralPolicy.accepts(w,h,hdr);
    if(bypass){status.report(hdr?"HDR 保持原始画面":"当前输入尺寸超出安全范围，保持原始画面",true);return new Size(w,h);}
    String extensions=GLES20.glGetString(GLES20.GL_EXTENSIONS);
    if(!GLES20.glGetString(GLES20.GL_VERSION).contains("OpenGL ES 3")&&(extensions==null||!extensions.contains("GL_OES_texture_half_float")))throw new IOException("GPU 缺少半浮点纹理支持");
    float scale=Math.min(2f,Math.min(displayWidth/(float)w,displayHeight/(float)h));outWidth=Math.max(2,Math.round(w*scale)/2*2);outHeight=Math.max(2,Math.round(h*scale)/2*2);
    if(gpuTimer!=null)gpuTimer.release();gpuTimer=new GpuTimer();levels=NeuralPolicy.levels(w,h);tier=NeuralPolicy.initial(levels);monitor.changed(android.os.SystemClock.elapsedRealtime(),false);inference(tier);
    status.report("AI 就绪 · 原图 "+w+"×"+h+" · "+width+"×"+height+" 推理",false);
    return new Size(outWidth,outHeight);
   }catch(Exception error){throw new VideoFrameProcessingException(error);}
  }
  void inference(int selected)throws GlUtil.GlException{
   clearTargets();if(gpuTimer!=null){gpuTimer.release();gpuTimer=new GpuTimer();}tier=selected;width=levels[tier][0];height=levels[tier][1];control.inferenceHeight=height;
   target("RGB");for(Pass pass:passes){target(pass.save);pass.program.setFloatsUniform("uStep",new float[]{1f/width,1f/height});}
   combine.setFloatsUniform("uSize",new float[]{width,height});combine.setFloatUniform("uGain",Math.min(1f,height/(float)Math.min(sourceHeight,1080)));
  }
  void target(String name)throws GlUtil.GlException{
   if(targets.containsKey(name))return;
   int[] ids=new int[1];GLES20.glGenTextures(1,ids,0);int texture=ids[0];int fbo=0;
   try{GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);boolean es3=GLES20.glGetString(GLES20.GL_VERSION).contains("OpenGL ES 3");GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,es3?0x881A:GLES20.GL_RGBA,width,height,0,GLES20.GL_RGBA,es3?0x140B:0x8D61,null);GlUtil.checkGlError();fbo=GlUtil.createFboForTexture(texture);if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new GlUtil.GlException("Half float framebuffer unavailable");targets.put(name,new Target(texture,fbo));}catch(GlUtil.GlException error){GLES20.glDeleteTextures(1,new int[]{texture},0);if(fbo!=0)GLES20.glDeleteFramebuffers(1,new int[]{fbo},0);throw error;}
  }
  void draw(GlProgram program)throws GlUtil.GlException{program.use();program.bindAttributesAndUniforms();GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GlUtil.checkGlError();}
  void bind(Target target){GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,target.fbo);GLES20.glViewport(0,0,width,height);}
  @Override public void drawFrame(int input,long time)throws VideoFrameProcessingException{
   int[] output=new int[1];GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,output,0);
   try{
    if(bypass){identity.setSamplerTexIdUniform("uTexture",input,0);draw(identity);return;}
    cadence.observe(time);boolean measure=frames>=20&&frames%15==0;double gpuMs=gpuTimer.poll();boolean query=measure&&gpuTimer.begin();boolean synchronous=measure&&!gpuTimer.supported;if(synchronous)GLES20.glFinish();long begin=System.nanoTime();
    bind(targets.get("RGB"));extract.setSamplerTexIdUniform("uTexture",input,0);draw(extract);
    for(Pass pass:passes){bind(targets.get(pass.save));for(int i=0;i<pass.binds.length;i++)pass.program.setSamplerTexIdUniform("u"+pass.binds[i],targets.get(pass.binds[i]).texture,i);draw(pass.program);}
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,output[0]);GLES20.glViewport(0,0,outWidth,outHeight);combine.setSamplerTexIdUniform("uOriginal",input,0);combine.setSamplerTexIdUniform("uPredicted",targets.get("SUBCONV1").texture,1);draw(combine);
    if(query)gpuTimer.end();
    if(synchronous||gpuMs>=0){
     if(synchronous)GLES20.glFinish();double ms=synchronous?(System.nanoTime()-begin)/1000000.:gpuMs;double fps=cadence.fps(),budget=NeuralPolicy.budget(fps,control.declaredFps);long now=android.os.SystemClock.elapsedRealtime();boolean drops=control.dropsAt>0&&now-control.dropsAt<5000;
     int action=monitor.sample(ms,budget,now,tier<levels.length-1,tier>0,drops);double average=monitor.average();
     android.util.Log.i("BallTVAI","adaptive original="+sourceWidth+"x"+sourceHeight+" inference="+width+"x"+height+" fps="+fps+" mean="+average+"ms budget="+budget+"ms action="+action);
     if(action==1||action==-1){inference(tier+(action==1?1:-1));monitor.changed(now,action==1);control.dropsAt=0;status.report("AI 自适应 · 原图 "+sourceWidth+"×"+sourceHeight+" · "+width+"×"+height+" 推理",false);}
     else if(action==2&&!stopReported){stopReported=true;status.report("最低推理负载仍持续超出实时能力，恢复原始画面",true);}
     else if(monitor.count>=5&&frames%60==0)status.report(String.format(java.util.Locale.ROOT,"AI 增强 · %dp推理 · 采样 %.1fms/帧 · 预算 %.1fms",height,average,budget),false);
    }
    frames++;
   }catch(Exception error){throw new VideoFrameProcessingException(error,time);}finally{GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,output[0]);}
  }
  void clearTargets(){for(Target t:targets.values()){GLES20.glDeleteTextures(1,new int[]{t.texture},0);GLES20.glDeleteFramebuffers(1,new int[]{t.fbo},0);}targets.clear();}
  @Override public void release()throws VideoFrameProcessingException{
   clearTargets();if(gpuTimer!=null)gpuTimer.release();try{if(identity!=null)identity.delete();if(extract!=null)extract.delete();if(combine!=null)combine.delete();for(Pass pass:passes)pass.program.delete();}catch(GlUtil.GlException error){throw new VideoFrameProcessingException(error);}finally{super.release();}
  }
 }
}
