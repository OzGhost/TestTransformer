package TestTransformer;

import java.util.Date;
import org.junit.runner.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.mockito.*;
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

    @Mock NonStaticSubject fnss;
    NonStaticSubject they;

    @Test
    public void duplicate_declare() {
        NonStaticSubject fnss = mock(NonStaticSubject.class);
        when(fnss.lift(eq(6))).thenReturn(2);
    }

    @Test
    public void test_mock_all_with_field_base() {
        mockStatic(NonStaticSubject.class);
        when(fnss.lift(eq(6))).thenReturn(2);
    }

    @Test
    public void test_mock_all() {
        mockStatic(NonStaticSubject.class);
        NonStaticSubject ss = mock(NonStaticSubject.class);
        when(NonStaticSubject.create(anyInt(), any(), anyList())).thenReturn(ss);
        when(ss.val()).thenReturn(16);
        doNothing().when(ss).noReturn();
        when(ss.lift(80)).thenReturn(20);
        when(ss.lift(eq(6))).thenReturn(4);

        mockStatic(StaticSubject.class);
        when(StaticSubject.getRefun()).thenReturn(8);
        PowerMockito.doNothing().when(StaticSubject.class);
        StaticSubject.noRefun();
        
        App t = new App();
        assertEquals(t.getVal(), 8+16+20+4);

        verifyStatic(NonStaticSubject.class, times(1));
        NonStaticSubject.create(anyInt(), any(Long.class), anyList());

        verify(ss, times(1)).val();
        verify(ss, times(1)).noReturn();
        verify(ss, times(1)).lift(80);
        verify(ss, times(1)).lift(6);

        verifyStatic(StaticSubject.class, times(1));
        StaticSubject.getRefun();
        verifyStatic(StaticSubject.class, times(1));
        StaticSubject.noRefun();
    }
}
