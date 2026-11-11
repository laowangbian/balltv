package cn.balltv.portal;
/** Search is a temporary layer over browsing; repeated searches share one origin. */
public final class SearchSession {
 public static final class Snapshot {
  public final String source,category,league,date,focus;public final int scroll;
  public Snapshot(String s,String c,String l,String d,int y,String f){source=s;category=c;league=l;date=d;scroll=y;focus=f;}
 }
 private Snapshot origin;
 public void begin(String query,Snapshot state){if(!query.trim().isEmpty()&&origin==null)origin=state;}
 public Snapshot origin(){return origin;}
 public void restore(Snapshot state){origin=state;}
 public Snapshot clear(){Snapshot state=origin;origin=null;return state;}
 public enum Back{SEARCH,FILTERS,HOME,ARM_EXIT,EXIT}
 public static Back back(String query,boolean filters,String view,boolean armed){if(!query.isEmpty())return Back.SEARCH;if(filters)return Back.FILTERS;if(!view.equals("live"))return Back.HOME;return armed?Back.EXIT:Back.ARM_EXIT;}
}
