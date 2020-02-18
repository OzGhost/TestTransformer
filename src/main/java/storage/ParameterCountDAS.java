package storage;

public class ParameterCountDAS extends SimpleDAS<Integer, MethodDesc> {

    public MethodDesc findByParameterCount(int pCount) {
        return find(pCount);
    }

    @Override
    protected MethodDesc createValue() {
        return new MethodDesc();
    }
}

