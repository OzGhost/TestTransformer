package worker;

import static meta.Name.ERROR;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import meta.CallDash;
import meta.CallGraph;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ClassScanner {

    public static CallGraph scanCallGraph(List<MethodDeclaration> methods) {
        List<String> rootCodes = new LinkedList<>();
        Set<String> nonTestSignatures = new HashSet<>();
        Map<String, CallDash> graph = new HashMap<>();

        for (MethodDeclaration mUnit: methods) {
            String mSig = SignatureService.extractSignature(mUnit);
            if (graph.containsKey(mSig)) {
                WoodLog.attach(ERROR, "Hit same method signature: " + mSig);
                continue;
            }
            graph.put(mSig, new CallDash(mUnit));
            if ( isRoot(mUnit) ) {
                rootCodes.add( mSig );
            } else {
                nonTestSignatures.add( mSig );
            }
        }
        for (Entry<String, CallDash> e: graph.entrySet()) {
            String rCode = e.getKey();
            CallDash dash = e.getValue();
            List<MethodDeclaration> callees = new LinkedList<>();
            List<MethodCallExpr> connectors = new LinkedList<>();
            List<String> calleesSignatures = new LinkedList<>();
            for (MethodCallExpr call: dash.getCaller().findAll(MethodCallExpr.class)) {
                String callSig = SignatureService.extractSignature(call);
                if (nonTestSignatures.contains(callSig)) {
                    callees.add( graph.get(callSig).getCaller() );
                    connectors.add(call);
                    calleesSignatures.add(callSig);
                }
            }
            int len = callees.size();
            dash.setCallees(callees.toArray(new MethodDeclaration[len]));
            dash.setConnectors(connectors.toArray(new MethodCallExpr[len]));
            dash.setCalleesSignatures(calleesSignatures.toArray(new String[len]));
        }
        return new CallGraph(graph, rootCodes);
    }

    private static boolean isRoot(MethodDeclaration mUnit) {
        for (AnnotationExpr annotation: mUnit.getAnnotations()) {
            String annotationName = annotation.getName().asString();
            if ("Test".equals(annotationName)) {
                return true;
            }
        }
        return false;
    }
}

