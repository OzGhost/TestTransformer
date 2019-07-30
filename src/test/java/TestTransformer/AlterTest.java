package TestTransformer;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;
import mockit.*;
import subject.*;

public class AlterTest {

    @Mocked NonStaticSubject nss;
    @Mocked StaticSubject ss;

    @Test
    public void test_mock_alternative_way() {
        int[] noRefunCounter = new int[1];
        int[] getRefunCounter = new int[1];
        new Expectations() {{
            NonStaticSubject.create(anyInt, (Long)any, (List)any); result = nss;
            nss.val(); result = 1928;
            nss.lift(80); result = 30;
            nss.lift(6); result = 4;
            StaticSubject.getRefun(); result = 9;
        }};
        
        App t = new App();
        assertEquals(t.getVal(), 9+1928+30+4);

        new Verifications() {{
            nss.val(); times = 1;
            nss.noReturn(); times = 1;
            NonStaticSubject.create(anyInt, (Long)any, (List)any); times = 1;
            StaticSubject.getRefun(); times = 1;
            StaticSubject.noRefun(); times = 1;
        }};
    }
}
