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
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.*;
import subject.NonStaticSubject;
import subject.StaticSubject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    NonStaticSubject.class,
    StaticSubject.class,
    App.class
})
public class MockTest {

    /*
    InvocationCounter abc, xyz, ckc;

    @Test
    public void test_invocationCounter() {
        verifyStatic(NonStaticSubject.class, ckc);
        NonStaticSubject.create(anyInt(), any(Long.class), anyList());
        assertEquals(4, ckc.times());

        verify(ss, abc).val();
        assertEquals(8, abc.times());
        verify(ss, abc).noReturn();
        assertEquals(9, abc.times());
    }
    */

    @Mock NonStaticSubject fnss;
    NonStaticSubject they;

    @Test
    public void duplicate_declare() {
        NonStaticSubject fnss = mock(NonStaticSubject.class);
        when(fnss.lift(eq(6))).thenReturn(2);
        new App().getVal();
    }

    @Test
    public void test_mock_all_with_field_base() {
        when(fnss.lift(eq(6))).thenReturn(2);
        new App().getVal();
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

    /*
    @Test
    public void test_fn01_private_mock() throws Exception {
        NonStaticSubject nss = new NonStaticSubject();
        nss = Mockito.spy(nss);
        whenNew(NonStaticSubject.class).withNoArguments().thenReturn(nss);
        PowerMockito.doReturn(35).when(nss, "pval");
        assertEquals(35, new App().fn01());
    }

    @Test
    @Ignore
    public void test_fn02_private_static_mock() throws Exception {
        PowerMockito.when(StaticSubject.class, "pval").thenReturn(66);
        PowerMockito.when(StaticSubject.class, "pval", 10).thenReturn(66);
        assertEquals(66, new App().fn02());
    }
    */

    @Test
    public void test_fn01_mock_when_new() throws Exception {
        NonStaticSubject nss = mock(NonStaticSubject.class);
        PowerMockito.whenNew(NonStaticSubject.class).withNoArguments().thenReturn(nss);
        when(nss.fval()).thenReturn(100);
        assertEquals(100, new App().fn01());
    }
}
