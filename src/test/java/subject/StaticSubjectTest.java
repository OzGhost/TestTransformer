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
    public void test_real_parameter_matching() {
        final int[] counter = new int[1];
        new MockUp<StaticSubject>() {
            @Mock
            public int rebase(Invocation ctx) {
                counter[0]++;
                int arg = (int)ctx.getInvokedArguments()[0];
                if (arg == 5) {
                    return 100;
                }
                if (arg == 7) {
                    return 2000;
                }
                return 0;
            }
        };

        assertEquals(100, StaticSubject.rebase(5));
        assertEquals(2000, StaticSubject.rebase(7));
        assertEquals(2, counter[0]);
    }

    private void print(Object o) {
        System.out.println("[]>>>>>>>>>>>>> " + o);
    }
}

