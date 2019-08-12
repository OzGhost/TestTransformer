package storage;

public class PackageDAS extends SimpleDAS<String, TypeDAS> {
    
    public TypeDAS findByPackage(String pkg) {
        return find(pkg);
    }

    @Override
    protected TypeDAS createValue() {
        return new TypeDAS();
    }
}

