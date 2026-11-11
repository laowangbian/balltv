package cn.balltv.portal;

import android.content.Context;
import android.opengl.GLES20;
import com.google.android.exoplayer2.effect.*;
import com.google.android.exoplayer2.util.*;

/** One bounded five-sample GPU pass. No CPU frame copies or model inference. */
final class ClarityEffect implements GlEffect {
 @Override public GlShaderProgram toGlShaderProgram(Context context,boolean hdr)throws VideoFrameProcessingException{return new Program(hdr);}
 static final class Program extends BaseGlShaderProgram {
  final GlProgram shader;
  Program(boolean hdr)throws VideoFrameProcessingException{
   super(hdr,1);
   try{
    shader=new GlProgram(
     "attribute vec4 aPosition; varying vec2 vTex; void main(){gl_Position=aPosition;vTex=aPosition.xy*0.5+0.5;}",
     "precision mediump float; uniform sampler2D uTexture; uniform vec2 uStep; uniform float uAmount; varying vec2 vTex; void main(){vec4 c=texture2D(uTexture,vTex);if(uAmount<0.001){gl_FragColor=c;return;}vec3 avg=(texture2D(uTexture,vTex+vec2(uStep.x,0.)).rgb+texture2D(uTexture,vTex-vec2(uStep.x,0.)).rgb+texture2D(uTexture,vTex+vec2(0.,uStep.y)).rgb+texture2D(uTexture,vTex-vec2(0.,uStep.y)).rgb)*0.25;vec3 delta=c.rgb-avg;float value=dot(delta,vec3(.2126,.7152,.0722));vec3 d=vec3(value);vec3 lo=min(c.rgb,avg),hi=max(c.rgb,avg);float edge=smoothstep(0.004,0.02,max(max(abs(d.r),abs(d.g)),abs(d.b)));gl_FragColor=vec4(clamp(c.rgb+clamp(d*uAmount*edge,vec3(-0.04),vec3(0.04)),max(vec3(0.),lo-vec3(.02)),min(vec3(1.),hi+vec3(.02))),c.a);}");
    shader.setBufferAttribute("aPosition",GlUtil.getNormalizedCoordinateBounds(),GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    shader.setFloatUniform("uAmount",hdr?0f:.55f);
   }catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}
  }
  @Override public Size configure(int width,int height){shader.setFloatsUniform("uStep",new float[]{1f/width,1f/height});return new Size(width,height);}
  @Override public void drawFrame(int texture,long time)throws VideoFrameProcessingException{
   try{shader.use();shader.setSamplerTexIdUniform("uTexture",texture,0);shader.bindAttributesAndUniforms();GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GlUtil.checkGlError();}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e,time);}
  }
  @Override public void release()throws VideoFrameProcessingException{try{super.release();}finally{try{shader.delete();}catch(GlUtil.GlException e){throw new VideoFrameProcessingException(e);}}}
 }
}
