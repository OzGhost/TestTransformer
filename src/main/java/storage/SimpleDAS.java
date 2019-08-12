package storage;

import java.util.Map;
import java.util.HashMap;

public abstract class SimpleDAS<U, T> {

    private Map<U, T> das = new HashMap<>();

    protected T find(U k) {
        T output = das.get(k);
        if (output == null) {
            output = createValue();
            das.put(k, output);
        }
        return output;
    }

    abstract protected T createValue();

}

