package cn.balltv.portal;
final class ThemePolicy {
 static boolean light(String mode,int hour){if("light".equals(mode))return true;if("dark".equals(mode))return false;return hour>=7&&hour<19;}
}
