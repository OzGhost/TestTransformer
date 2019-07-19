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
}
