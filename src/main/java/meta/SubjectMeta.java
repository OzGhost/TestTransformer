package meta;

import worker.WoodLog;
import java.util.*;
import java.util.Map.Entry;

public class SubjectMeta implements Iterable<Entry<String, List<CallMeta>>> {

    private Map<String, List<CallMeta>> methodMetas = new HashMap<>();
    private String pkg = "";

    public Map<String, List<CallMeta>> getMethodMetas() {
        return methodMetas;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String p) {
        pkg = p;
    }

    public List<CallMeta> getByMethodName(String methodName) {
        List<CallMeta> out = methodMetas.get(methodName);
        if (out == null) {
            out = new ArrayList<>();
            methodMetas.put(methodName, out);
        }
        return out;
    }

    public void merge(SubjectMeta outmeta) {
        for (Entry<String, List<CallMeta>> e: outmeta.getMethodMetas().entrySet()) {
            String ek = e.getKey();
            List<CallMeta> storedMetas = methodMetas.get(ek);
            if (storedMetas == null) {
                methodMetas.put(ek, e.getValue());
            } else {
                storedMetas.addAll(e.getValue());
            }
        }
        if (pkg != null && outmeta.pkg != null) {
            pkg = outmeta.pkg;
        }
    }

    @Override
    public Iterator<Entry<String, List<CallMeta>>> iterator() {
        return methodMetas.entrySet().iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Iterator<Map.Entry<String, List<CallMeta>>> ite = methodMetas.entrySet().iterator();
        Map.Entry<String, List<CallMeta>> mmm;
        if (ite.hasNext()) {
            concatenate(ite.next(), sb);
        }
        while (ite.hasNext()) {
            WoodLog.loopLog(this, 62);
            sb.append(',');
            concatenate(ite.next(), sb);
        }
        sb.append('}');
        return sb.toString();
    }

    private void concatenate(Map.Entry<String, List<CallMeta>> item, StringBuilder sb) {
        sb.append('"')
            .append(item.getKey())
            .append("\":[");
        Iterator<CallMeta> ite = item.getValue().iterator();
        if (ite.hasNext()) {
            sb.append('"').append(ite.next().toString()).append('"');
        }
        while (ite.hasNext()) {
            WoodLog.loopLog(this, 79);
            sb.append(",\"").append(ite.next().toString()).append('"');
        }
        sb.append(']');
    }
}
