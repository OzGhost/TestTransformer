package TestTransformer;

import org.junit.*;
import static org.junit.Assert.*;
import mockit.*;
import subject.*;

public class AlterTest {

    @Test
    public void test_mock_alternative_way(@Mocked NonStaticSubject nss) {
        new MockUp<NonStaticSubject>() {
            @Mock
            public NonStaticSubject create() {
                return nss;
            }
        };
        new MockUp<StaticSubject>() {
            @Mock
            public int getRefun(Invocation inv) {
                assertEquals(inv.getInvocationCount(), 1);
                return 9;
            }
            @Mock
            public void noRefun(Invocation inv) {
                assertEquals(inv.getInvocationCount(), 1);
                // doNothing
            }
        };
        new Expectations() {{ nss.val(); result = 1928; }};
        
        App t = new App();
        assertEquals(t.getVal(), 9+1928);

        new Verifications() {{
            nss.val(); times = 1;
            nss.noReturn(); times = 1;
        }};
    }
}
