package meta;

import worker.SignatureService;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class CallDash {

    private MethodDeclaration caller;
    private MethodDeclaration[] callees;
    private MethodCallExpr[] connectors;
    private String[] calleesSignatures;

    public CallDash(MethodDeclaration cer) {
        caller = cer;
    }

    public MethodDeclaration getCaller() {
        return caller;
    }

    public MethodDeclaration[] getCallees() {
        return callees;
    }

    public void setCallees(MethodDeclaration[] c) {
        callees = c;
    }

    public MethodCallExpr[] getConnectors() {
        return connectors;
    }

    public void setConnectors(MethodCallExpr[] c) {
        connectors = c;
    }

    public String[] getCalleesSignatures() {
        return calleesSignatures;
    }

    public void setCalleesSignatures(String[] sigs) {
        calleesSignatures = sigs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( SignatureService.extractSignature(caller) ).append(" -> [ ");
        int len = callees.length;
        for (int i = 0; i < len; ++i) {
            sb.append( SignatureService.extractSignature(callees[i]) ).append(" , ");
        }
        sb.append(" ]");
        return sb.toString();
    }
}

