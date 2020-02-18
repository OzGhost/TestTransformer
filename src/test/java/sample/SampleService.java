
package sample;

public class SampleService {

    private static final String secret = "The top secret";

    public static SampleService createInstance() {
        return new SampleService();
    }

    public String getSecret() {
        return secret;
    }

    public static String getName() {
        return "prefix-" + getPrivateName();
    }

    private static String getPrivateName() {
        return "3man3ht";
    }

    public static void powerUp(int[] x) throws Exception {
        x[0] = x[0]*x[0];
    }
}

