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

    public boolean isEmpty() {
        return das.isEmpty();
    }

    public void load(SimpleDAS<U, T> i) {
        das = i.das;
        if (das == null) {
            das = new HashMap<>();
        }
    }

    public void merge(SimpleDAS<U, T> i) {
        if (i == null) {
            return;
        }
        for (Map.Entry<U, T> e: i.das.entrySet()) {
            das.put(e.getKey(), e.getValue());
        }
    }
}

