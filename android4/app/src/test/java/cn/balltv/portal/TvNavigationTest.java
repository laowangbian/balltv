package cn.balltv.portal;
import org.junit.Test;import static org.junit.Assert.*;
public class TvNavigationTest {
 @Test public void leftEdgeReturnsToSidebarInsteadOfDifferentRow(){assertEquals(-1,TvNavigation.target(4,21,4,12));assertEquals(4,TvNavigation.target(5,21,4,12));}
 @Test public void rightEdgeDoesNotJumpToNextRow(){assertEquals(3,TvNavigation.target(3,22,4,12));assertEquals(6,TvNavigation.target(5,22,4,12));}
 @Test public void verticalMovementStaysInColumn(){assertEquals(9,TvNavigation.target(5,20,4,12));assertEquals(1,TvNavigation.target(5,19,4,12));}
 @Test public void missingCellsAndBoundaryHaveDeterministicTargets(){assertEquals(9,TvNavigation.target(7,20,4,10));assertEquals(10,TvNavigation.target(9,20,4,10));assertEquals(-1,TvNavigation.target(0,19,4,10));}
 @Test public void scheduleRowsUseSingleColumn(){assertEquals(3,TvNavigation.target(2,20,1,8));assertEquals(1,TvNavigation.target(2,19,1,8));}
}
