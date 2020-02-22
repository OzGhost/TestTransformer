package meta;

import worker.WoodLog;
import java.util.*;
import java.util.Map.Entry;

public class MockingMeta implements Iterable<Entry<String, SubjectMeta>> {
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

    public boolean containsSubject(String subjectName) {
        return subjectMetas.get(subjectName) != null;
    }

    public boolean isEmpty() {
        return subjectMetas.isEmpty();
    }

    public void loadEntry(Entry<String, SubjectMeta> entry) {
        subjectMetas.put(entry.getKey(), entry.getValue());
    }

    public void mergeSubjectMeta(String subjectName, String pkg, SubjectMeta meta) {
        SubjectMeta storedMeta = subjectMetas.get(subjectName);
        meta.setPkg(pkg);
        if (storedMeta == null) {
            subjectMetas.put(subjectName, meta);
        } else {
            storedMeta.merge(meta);
        }
    }

    @Override
    public Iterator<Entry<String, SubjectMeta>> iterator() {
        return subjectMetas.entrySet().iterator();
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
            WoodLog.loopLog(this, 59);
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

    public List<Craft> toCrafts() {
        List<Craft> crafts = new LinkedList<>();
        Craft travelCraft = new Craft();
        for (Map.Entry<String, SubjectMeta> meta: subjectMetas.entrySet()) {
            travelCraft.setSubjectName(meta.getKey());
            SubjectMeta subjectMeta = meta.getValue();
            for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMetas().entrySet()) {
                travelCraft.setMethodName(mm.getKey());
                List<CallMeta> callMetas = mm.getValue();
                for (CallMeta cm: callMetas) {
                    travelCraft.setCallMeta(cm);
                    crafts.add(travelCraft.shadow());
                }
            }
        }
        return crafts;
    }
}

