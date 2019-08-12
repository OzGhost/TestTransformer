package storage;

public class TypeDAS extends SimpleDAS<String, MethodDAS> {

    public MethodDAS findByType(String type) {
        return find(type);
    }

    @Override
    protected MethodDAS createValue() {
        return new MethodDAS();
    }
}

