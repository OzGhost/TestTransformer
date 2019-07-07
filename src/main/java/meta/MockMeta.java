package meta;

import java.util.*;

public class MockMeta {
    private Map<String, SubjectMockMeta> subjectMockMetas = new HashMap<>();

    public Map<String, SubjectMockMeta> getSubjectMockMetas() {
        return subjectMockMetas;
    }

    public SubjectMockMeta getBySubjectName(String subjectName) {
        SubjectMockMeta smm = subjectMockMetas.get(subjectName);
        if (smm == null) {
            smm = new SubjectMockMeta();
            subjectMockMetas.put(subjectName, smm);
        }
        return smm;
    }
}
