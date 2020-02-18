
package sample;

public class SampleCaller {
    
    public static String callForSecret() {
        return SampleService.createInstance().getSecret();
    }

    public static String callForName() {
        return SampleService.getName();
    }

    public static String callForPower() throws Exception {
        int[] x = new int[1];
        x[0] = 10;
        SampleService.powerUp(x);
        return "" + x[0];
    }
}

