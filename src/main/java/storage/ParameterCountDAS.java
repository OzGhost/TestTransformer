package storage;

public class ParameterCountDAS extends SimpleDAS<Integer, ParameterPack> {

    public ParameterPack findByParameterCount(int pCount) {
        return find(pCount);
    }

    @Override
    protected ParameterPack createValue() {
        return new ParameterPack();
    }
}

