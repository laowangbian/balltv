package cn.balltv.portal;

import android.content.Context;
import java.util.*;

/** Local opt-out only: saved matches and watch history are never deleted. */
final class SourcePreferences {
 static Set<String> disabled(Context context) {
  return new HashSet<String>(context.getSharedPreferences("sources",0).getStringSet("disabled",Collections.<String>emptySet()));
 }
 static boolean enabled(Context context,String name) { return !context.getSharedPreferences("sources",0).getStringSet("disabled",Collections.<String>emptySet()).contains(name); }
 static void setEnabled(Context context,String name,boolean enabled) {
  Set<String> names=disabled(context);
  if(enabled)names.remove(name);else names.add(name);
  context.getSharedPreferences("sources",0).edit().putStringSet("disabled",names).apply();
 }
 static ArrayList<Feeds.Match> filter(Collection<Feeds.Match> matches,Set<String> disabled) {
  ArrayList<Feeds.Match> result=new ArrayList<Feeds.Match>();
  for(Feeds.Match match:matches)if(!disabled.contains(match.source))result.add(match);
  return result;
 }
 static ArrayList<Feeds.Match> filter(Context context,Collection<Feeds.Match> matches) {return filter(matches,disabled(context));}
}
