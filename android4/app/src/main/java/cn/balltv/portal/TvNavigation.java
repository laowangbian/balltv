package cn.balltv.portal;
/** Logical card movement independent of child text and screenshot geometry. */
public final class TvNavigation {
 public static final int LEFT=21,RIGHT=22,UP=19,DOWN=20;
 public static int target(int index,int key,int columns,int count){if(index<0||index>=count||columns<1)return -1;int target=index;switch(key){case LEFT:if(index%columns==0)return -1;target--;break;case RIGHT:if(index%columns==columns-1||index+1>=count)return index;target++;break;case UP:target=index-columns;break;case DOWN:target=Math.min(count-1,index+columns);if(index/columns==(count-1)/columns)return count;break;default:return index;}return target<0?-1:target;}
}
