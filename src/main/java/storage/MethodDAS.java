package storage;

import java.util.Map;

public class MethodDAS extends SimpleDAS<String, ParameterCountDAS> {

    public ParameterCountDAS findByMethod(String methodName) {
        return find(methodName);
    }

    @Override
    protected ParameterCountDAS createValue() {
        return new ParameterCountDAS();
    }
}

