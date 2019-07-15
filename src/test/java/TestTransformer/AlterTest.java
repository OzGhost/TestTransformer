package TestTransformer;

import org.junit.*;
import static org.junit.Assert.*;
import mockit.*;
import subject.*;

public class AlterTest {

    @Test
    public void test_mock_alternative_way(@Mocked NonStaticSubject nss, @Mocked StaticSubject ss) {
        int[] noRefunCounter = new int[1];
        int[] getRefunCounter = new int[1];
        new Expectations() {{
            nss.val(); result = 1928;
            nss.lift(); result = 30;
            StaticSubject.getRefun(); result = 9;
        }};
        
        App t = new App();
        assertEquals(t.getVal(), 9+1928+30);
        //assertEquals(1, getRefunCounter[0]);
        //assertEquals(1, noRefunCounter[0]);

        new Verifications() {{
            nss.val(); times = 1;
            nss.noReturn(); times = 1;
            NonStaticSubject.create(anyInt); times = 1;
            StaticSubject.getRefun(); times = 1;
            StaticSubject.noRefun(); times = 1;
        }};
    }
}
