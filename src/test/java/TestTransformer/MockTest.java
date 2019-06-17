package TestTransformer;

import org.junit.runner.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.*;
import subject.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    NonStaticSubject.class,
    StaticSubject.class
})
public class MockTest {
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
    }
}
