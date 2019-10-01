package meta;

import java.util.List;
import worker.SignatureService;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class CallDash {

    private MethodDeclaration caller;
    private String callerSignature;
    private MethodDeclaration[] callees;
    private MethodCallExpr[] connectors;
    private String[] calleeSignatures;

    public CallDash(MethodDeclaration cer, String cerSig) {
        caller = cer;
        callerSignature = cerSig;
    }

    public MethodDeclaration getCaller() {
        return caller;
    }

    public String getCallerSignature() {
        return callerSignature;
    }

    public MethodDeclaration[] getCallees() {
        return callees;
    }

    public void setCallees(List<MethodDeclaration> c) {
        setCallees(c.toArray(new MethodDeclaration[c.size()]));
    }

    public void setCallees(MethodDeclaration[] c) {
        callees = c;
    }

    public MethodCallExpr[] getConnectors() {
        return connectors;
    }

    public void setConnectors(List<MethodCallExpr> c) {
        setConnectors(c.toArray(new MethodCallExpr[c.size()]));
    }

    public void setConnectors(MethodCallExpr[] c) {
        connectors = c;
    }

    public String[] getCalleeSignatures() {
        return calleeSignatures;
    }

    public void setCalleeSignatures(List<String> sigs) {
        setCalleeSignatures(sigs.toArray(new String[sigs.size()]));
    }

    public void setCalleeSignatures(String[] sigs) {
        calleeSignatures = sigs;
    }

    public boolean isEndDash() {
        return callees == null || callees.length == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( callerSignature ).append(" -> [ ");
        int len = calleeSignatures.length;
        for (int i = 0; i < len; ++i) {
            sb.append( calleeSignatures[i] ).append(" , ");
        }
        sb.append(" ]");
        return sb.toString();
    }
}

