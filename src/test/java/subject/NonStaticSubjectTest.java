package subject;

import org.junit.*;
import static org.junit.Assert.*;
import mockit.*;

public class NonStaticSubjectTest {

    @Test
    public void test_self() {
        NonStaticSubject nss = new NonStaticSubject();

        assertEquals(99, nss.lift(28));
        assertEquals(150, nss.lift(59182));
    }

    @Test
    public void test_param_spliter(@Mocked NonStaticSubject nss) {
        new Expectations(){{
            nss.lift(28); result = 85;
            nss.lift(59182); result = 59;
        }};

        assertEquals(85, nss.lift(28));
        assertEquals(59, nss.lift(59182));
    }
}
