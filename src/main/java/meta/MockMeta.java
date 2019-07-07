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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Iterator<Map.Entry<String, SubjectMockMeta>> ite = subjectMockMetas.entrySet().iterator();
        Map.Entry<String, SubjectMockMeta> smm;
        if (ite.hasNext()) {
            concatenate(ite.next(), sb);
        }
        while (ite.hasNext()) {
            sb.append(',');
            concatenate(ite.next(), sb);
        }
        sb.append('}');
        return sb.toString();
    }

    private void concatenate(Map.Entry<String, SubjectMockMeta> item, StringBuilder sb) {
        sb.append('"')
            .append(item.getKey())
            .append('"')
            .append(':')
            .append(item.getValue().toString());
    }
}
