package meta;

import java.util.*;

public class MockingMeta {
    private Map<String, SubjectMeta> subjectMetas = new LinkedHashMap<>();

    public Map<String, SubjectMeta> getSubjectMetas() {
        return subjectMetas;
    }

    public SubjectMeta getBySubjectName(String subjectName) {
        SubjectMeta smm = subjectMetas.get(subjectName);
        if (smm == null) {
            smm = new SubjectMeta();
            subjectMetas.put(subjectName, smm);
        }
        return smm;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Iterator<Map.Entry<String, SubjectMeta>> ite = subjectMetas.entrySet().iterator();
        Map.Entry<String, SubjectMeta> smm;
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

    private void concatenate(Map.Entry<String, SubjectMeta> item, StringBuilder sb) {
        sb.append('"')
            .append(item.getKey())
            .append('"')
            .append(':')
            .append(item.getValue().toString());
    }
}
