
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
public class StaticReturnMockTest {

    @Test
    public void test_static_return_mock() {
        mockStatic(SampleService.class);
        when(SampleService.getName()).thenReturn("p-s");

        String o = SampleCaller.callForName();
        assertEquals("p-s", o);
    }
}

