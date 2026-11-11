package cn.balltv.portal;
import android.opengl.GLES20;import android.opengl.GLES30;import java.util.*;
/** Nonblocking GPU elapsed queries when available; never count a preceding GPU backlog as one frame. */
final class GpuTimer {
 static final int TIME_ELAPSED=0x88BF,RESULT=0x8866,AVAILABLE=0x8867,DISJOINT=0x8FBB;
 boolean supported;int active;final ArrayDeque<Integer> pending=new ArrayDeque<Integer>();
 GpuTimer(){String extensions=GLES20.glGetString(GLES20.GL_EXTENSIONS);supported=extensions!=null&&extensions.contains("GL_EXT_disjoint_timer_query")&&GLES20.glGetString(GLES20.GL_VERSION).contains("OpenGL ES 3");}
 boolean begin(){if(!supported||active!=0||pending.size()>=3)return false;int[] query=new int[1];GLES30.glGenQueries(1,query,0);GLES30.glBeginQuery(TIME_ELAPSED,query[0]);if(GLES20.glGetError()!=GLES20.GL_NO_ERROR){GLES30.glDeleteQueries(1,query,0);supported=false;return false;}active=query[0];return true;}
 void end(){if(active!=0){GLES30.glEndQuery(TIME_ELAPSED);pending.addLast(active);active=0;}}
 double poll(){if(!supported||pending.isEmpty())return -1;int id=pending.peekFirst();int[] value=new int[1];GLES30.glGetQueryObjectuiv(id,AVAILABLE,value,0);if(value[0]==0)return -1;pending.removeFirst();GLES20.glGetIntegerv(DISJOINT,value,0);boolean invalid=value[0]!=0;GLES30.glGetQueryObjectuiv(id,RESULT,value,0);double millis=(value[0]&0xffffffffL)/1000000.;GLES30.glDeleteQueries(1,new int[]{id},0);return invalid||millis<=0?-1:millis;}
 void release(){if(active!=0){GLES30.glEndQuery(TIME_ELAPSED);pending.add(active);active=0;}while(!pending.isEmpty())GLES30.glDeleteQueries(1,new int[]{pending.removeFirst()},0);}
}
