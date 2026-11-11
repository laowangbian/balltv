package cn.balltv.portal;
import org.junit.*;import static org.junit.Assert.*;
public class RequestBudgetTest {
 @After public void clear(){RequestBudget.clear();}
 @Test public void nestedOperationsCannotRestartAnExpiredVerificationDeadline(){RequestBudget.begin(-1);assertTrue(RequestBudget.expired());long normal=System.nanoTime()+28000000000L;assertTrue(RequestBudget.deadline(normal)<normal);RequestBudget.clear();assertEquals(normal,RequestBudget.deadline(normal));}
 @Test public void foregroundPlaybackDoesNotInheritBackgroundThreadDeadline()throws Exception{RequestBudget.begin(-1);boolean[] expired={true};Thread t=new Thread(()->expired[0]=RequestBudget.expired());t.start();t.join();assertFalse(expired[0]);assertTrue(RequestBudget.expired());}
}
