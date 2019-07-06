package TestTransformer;

import java.util.Date;
import org.junit.runner.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.*;
import subject.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    NonStaticSubject.class,
    StaticSubject.class
})
public class MockTest {

    /*
    public void test_of_the_test(int iv1, Date iv2) {
        int cv1 = iv1;
        Date cv2 = iv2;
    }
    */

    @Test
    public void test_mock_all() {
        mockStatic(NonStaticSubject.class);
        NonStaticSubject ss = mock(NonStaticSubject.class);
        when(NonStaticSubject.create()).thenReturn(ss);
        when(ss.val()).thenReturn(16);
        doNothing().when(ss).noReturn();

        mockStatic(StaticSubject.class);
        when(StaticSubject.getRefun()).thenReturn(8);
        PowerMockito.doNothing().when(StaticSubject.class);
        StaticSubject.noRefun();
        
        App t = new App();
        assertEquals(t.getVal(), 8+16);

        verify(ss, times(1)).val();
        verify(ss, times(1)).noReturn();
        verifyStatic(times(1));
        StaticSubject.getRefun();
        verifyStatic(times(1));
        StaticSubject.noRefun();
    }
}
