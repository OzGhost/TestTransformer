package meta;

public class Craft {
    private String subjectName;
    private String methodName;
    private CallMeta callMeta;

    public void setSubjectName(String name) {
        subjectName = name;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setMethodName(String name) {
        methodName = name;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setCallMeta(CallMeta meta) {
        callMeta = meta;
    }

    public CallMeta getCallMeta() {
        return callMeta;
    }

    public Craft shadow() {
        Craft c = new Craft();
        c.subjectName = subjectName;
        c.methodName = methodName;
        c.callMeta = callMeta;
        return c;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(subjectName).append(" : ").append(methodName).append(" : ").append(callMeta.toString());
        return sb.toString();
    }
}
