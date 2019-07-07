package meta;

import java.util.*;

public class SubjectMockMeta {
    private Map<String, List<CoreMockMeta>> methodMockMetas = new HashMap<>();

    public Map<String, List<CoreMockMeta>> getMethodMockMetas() {
        return methodMockMetas;
    }

    public List<CoreMockMeta> getByMethodName(String methodName) {
        List<CoreMockMeta> out = methodMockMetas.get(methodName);
        if (out == null) {
            out = new ArrayList<>();
            methodMockMetas.put(methodName, out);
        }
        return out;
    }
}
