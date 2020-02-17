
package cases;

import sample.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SampleService.class)
public class StaticCreateWithNormalCallTest {

    @Test
    public void test_static_creation_with_normal_call() {
        System.out.println("base test running");
        mockStatic(SampleService.class);
        SampleService service = mock(SampleService.class);
        when(SampleService.createInstance()).thenReturn(service);
        when(service.getSecret()).thenReturn("another secret");

        String output = SampleCaller.callForSecret();
        assertEquals("another secret", output);
    }

    @Test
    public void test_static_creation_with_normal_call_explicit_ver() {
        PowerMockito.mockStatic(SampleService.class);
        SampleService service = Mockito.mock(SampleService.class);
        Mockito.when(SampleService.createInstance()).thenReturn(service);
        Mockito.when(service.getSecret()).thenReturn("another secret");

        String output = SampleCaller.callForSecret();
        assertEquals("another secret", output);
    }

    @Test
    public void test_static_creation_with_normal_call_explicit_advance_ver() {
        PowerMockito.mockStatic(sample.SampleService.class);
        sample.SampleService service = Mockito.mock(sample.SampleService.class);
        Mockito.when(sample.SampleService.createInstance()).thenReturn(service);
        Mockito.when(service.getSecret()).thenReturn("another secret");

        String output = sample.SampleCaller.callForSecret();
        assertEquals("another secret", output);
    }
}

