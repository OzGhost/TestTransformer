
package sample;

public class SampleService {

    private static final String secret = "The top secret";

    public static SampleService createInstance() {
        return new SampleService();
    }

    public String getSecret() {
        return secret;
    }
}

