
package cases;

import sample.SampleService;
import sample.SampleCaller;
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
public class StaticVoidMockTest {
    
    @Test
    public void test_static_void_mock() throws Exception {
        mockStatic(SampleService.class);
        PowerMockito.doNothing().when(SampleService.class);
        SampleService.powerUp(any(int[].class));

        String o = SampleCaller.callForPower();
        assertEquals("10", o);
    }

    @Test
    public void test_static_void_mock_indirect_func_spec() throws Exception {
        mockStatic(SampleService.class);
        PowerMockito.doNothing().when(SampleService.class, "powerUp", any(int[].class));

        String o = SampleCaller.callForPower();
        assertEquals("10", o);
    }
}

