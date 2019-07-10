package worker;

import meta.*;
import java.util.*;
import com.github.javaparser.ast.stmt.*;

public class InstanceMockWorker {
    private List<Map.Entry<String, SubjectMockMeta>> metas = new ArrayList<>();

    public void addMockMeta(Map.Entry<String, SubjectMockMeta> mockMeta) {
        metas.add(mockMeta);
    }

    public boolean isEmpty() {
        return metas.isEmpty();
    }

    public Statement transform() {
        return null;
    }
}

