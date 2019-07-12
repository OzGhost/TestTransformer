package subject;

import org.junit.*;
import static org.junit.Assert.*;
import mockit.*;

public class StaticSubjectTest {

    @Test
    public void test_function_content() {
        assertEquals(StaticSubject.rebase(5), -1);
        assertEquals(StaticSubject.rebase(923), 1);
    }

    @Test
    public void test_real_parameter_matching(@Mocked StaticSubject ss) {
        final int[] counter = new int[1];
        new Expectations(){{
            StaticSubject.rebase(5); result = 100;
            StaticSubject.rebase(7); result = 2000;
        }};

        assertEquals(100, StaticSubject.rebase(5));
        assertEquals(2000, StaticSubject.rebase(7));
        //assertEquals(2, counter[0]);
    }

    private void print(Object o) {
        System.out.println("[]>>>>>>>>>>>>> " + o);
    }
}

