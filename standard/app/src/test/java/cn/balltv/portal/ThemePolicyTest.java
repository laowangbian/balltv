package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class ThemePolicyTest {
 @Test public void automaticChangesAtSevenAndNineteen(){assertFalse(ThemePolicy.light("auto",6));assertTrue(ThemePolicy.light("auto",7));assertTrue(ThemePolicy.light("auto",18));assertFalse(ThemePolicy.light("auto",19));assertFalse(ThemePolicy.light("auto",0));}
 @Test public void manualChoiceOverridesClock(){for(int hour=0;hour<24;hour++){assertTrue(ThemePolicy.light("light",hour));assertFalse(ThemePolicy.light("dark",hour));}}
}
