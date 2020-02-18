
package storage;

public class MethodDesc {
    private String[] returnType;
    private String[][] paramTypes;
    private String[] exceptions;

    public String[] getReturnType() {
        return returnType;
    }

    public void setReturnType(String[] rt) {
        returnType = rt;
    }

    public String[][] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(String[][] pts) {
        paramTypes = pts;
    }

    public String[] getExceptions() {
        return exceptions;
    }

    public void setExceptions(String[] exs) {
        exceptions = exs;
    }
}

