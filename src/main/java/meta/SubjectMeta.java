package meta;

import java.util.*;

public class SubjectMeta {
    private Map<String, List<CallMeta>> methodMetas = new HashMap<>();

    public Map<String, List<CallMeta>> getMethodMetas() {
        return methodMetas;
    }

    public List<CallMeta> getByMethodName(String methodName) {
        List<CallMeta> out = methodMetas.get(methodName);
        if (out == null) {
            out = new ArrayList<>();
            methodMetas.put(methodName, out);
        }
        return out;
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
            sb.append(",\"").append(ite.next().toString()).append('"');
        }
        sb.append(']');
    }
}
