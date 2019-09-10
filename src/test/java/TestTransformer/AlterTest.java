package TestTransformer;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.List;
import mockit.*;
import subject.*;

public class AlterTest {

    @Test
    public void test_mock_alternative_way(
            @Mocked NonStaticSubject nss,
            @Mocked StaticSubject ss
    ) {
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

    @Test
    public void test_mock_alternative_way_via_helper(
            @Mocked NonStaticSubject nss,
            @Mocked StaticSubject ss
    ) {
        helper(nss, ss);
        App t = new App();
        assertEquals(t.getVal(), 9+1928+30+4);
    }

    private void helper(
            NonStaticSubject nss,
            StaticSubject ss
    ) {
        new Expectations() {{
            NonStaticSubject.create(anyInt, (Long)any, (List)any); result = nss;
            nss.val(); result = 1928;
            nss.lift(80); result = 30;
            nss.lift(6); result = 4;
            StaticSubject.getRefun(); result = 9;
        }};
    }

    /*
    @Test
    public void test_fn02_private_static_mock() {
        new MockUp<StaticSubject>() {
            @Mock
            int pval() {
                return 66;
            }
        };
        assertEquals(66, new App().fn02());
    }
    */

    @Test
    public void test_fn03_whenNew(@Mocked NonStaticSubject nss) throws Exception {
        new Expectations() {{
            nss.fval(); result = 25;
            nss.sval(); result = 100;
        }};
        assertEquals(125, new App().fn03());
    }

    @Test
    public void test_fn03_spy_v1() throws Exception {
        NonStaticSubject a = new NonStaticSubject();
        new Expectations(a) {{
            NonStaticSubject.create(anyInt, (Long)any, (List)any); result = a;
            a.fval(); result = 25;
        }};
        assertEquals(60, new App().fn03_2());
    }

    @Test
    public void test_fn03_spy_v2(@Mocked StaticSubject ss) throws Exception {
        NonStaticSubject nss = new NonStaticSubject();
        new Expectations(nss) {{
            StaticSubject.getNext(); result = nss;
            nss.sval(); result = 1;
        }};
        assertEquals(23, new App().fn03_1());
    }

    @Test
    public void test_fn03_spy_v3(@Mocked StaticSubject ss) throws Exception {
        NonStaticSubject nss = new NonStaticSubject(20);
        new Expectations(nss) {{
            StaticSubject.getNext(); result = nss;
            nss.fval(); result = 1;
        }};
        assertEquals(21, new App().fn03_1());
    }

    @Test
    public void test_fn03_1_verify_without_mocked_or_spy(@Mocked StaticSubject ss) throws Exception {
        NonStaticSubject nss = new NonStaticSubject();
        new Expectations() {{
            StaticSubject.getNext(); result = nss;
        }};
        assertEquals(57, new App().fn03_1());
        new Verifications() {{
            nss.fval(); times = 1;
            nss.sval(); times = 1;
        }};
    }

    @Test
    public void test_fn04_suppress_void_over_spy(@Mocked StaticSubject ss) throws Exception {
        NonStaticSubject nss = new NonStaticSubject();
        new Expectations(nss) {{
            StaticSubject.getNext(); result = nss;
            nss.sval(); result = 22;
            nss.reset();
        }};
        assertEquals(44, new App().fn04());
    }

    @Test
    public void test_fn06_suppress_static_void() throws Exception {
        new MockUp<StaticSubject>() {
            @Mock
            void fn06_private() {
                //do nothing
            }
            @Mock
            int fn05() {
                return 10;
            }
        };
        new App().fn06();
    }
}
