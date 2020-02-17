
package sample;

public class SampleCaller {
    
    public static String callForSecret() {
        return SampleService.createInstance().getSecret();
    }
}

