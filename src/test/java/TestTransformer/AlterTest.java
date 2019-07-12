package TestTransformer;

import org.junit.*;
import static org.junit.Assert.*;
import mockit.*;
import subject.*;

public class AlterTest {

    @Test
    public void test_mock_alternative_way(@Mocked NonStaticSubject nss) {
        int[] noRefunCounter = new int[1];
        int[] getRefunCounter = new int[1];
        new MockUp<NonStaticSubject>() {
            @Mock
            public NonStaticSubject create() {
                return nss;
            }
        };
        new MockUp<StaticSubject>() {
            @Mock
            int getRefun(Invocation inv) {
                getRefunCounter[0]++;
                return 9;
            }
            @Mock
            void noRefun() {
                noRefunCounter[0]++;
            }
        };
        new Expectations() {{ nss.val(); result = 1928; }};
        
        App t = new App();
        assertEquals(t.getVal(), 9+1928);
        assertEquals(1, getRefunCounter[0]);
        assertEquals(1, noRefunCounter[0]);

        new Verifications() {{
            nss.val(); times = 1;
            nss.noReturn(); times = 1;
        }};
    }
}
