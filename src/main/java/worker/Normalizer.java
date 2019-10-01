package worker;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Deque;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import meta.CallDash;
import reader.ReaderUtil;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.NodeList;

public class Normalizer {

    private static final AtomicInteger PARAM_INDEX = new AtomicInteger();
    
    public static ClassOrInterfaceDeclaration normalize(ClassOrInterfaceDeclaration rawClass) {
        Map<String, CallDash> graph = new HashMap<>();
        for (BodyDeclaration<?> mem: rawClass.getMembers()) {
            if ( ! (mem instanceof MethodDeclaration) ) continue;
            MethodDeclaration method = (MethodDeclaration) mem;
            String methodSig = SignatureService.extractSignature(method);
            CallDash dash = new CallDash(method, methodSig);
            graph.putIfAbsent(methodSig, dash);
        }

        for (CallDash dash: graph.values()) {
            MethodDeclaration method = dash.getCaller();
            List<MethodDeclaration> callees = new LinkedList<>();
            List<MethodCallExpr> connectors = new LinkedList<>();
            List<String> callSigs = new LinkedList<>();
            for (MethodCallExpr call: method.findAll(MethodCallExpr.class)) {
                String callSig = SignatureService.extractSignature(call);
                CallDash calleeDash = graph.get(callSig);
                if (calleeDash == null) continue;
                callees.add(calleeDash.getCaller());
                connectors.add(call);
                callSigs.add(callSig);
            }
            dash.setCallees(callees);
            dash.setConnectors(connectors);
            dash.setCalleeSignatures(callSigs);

            // parameter rename
            List<String> params = new LinkedList<>();
            for (Parameter p: method.getParameters()) {
                String pname = p.getName().asString();
                params.add(pname);
            }
            for (String p: params) {
                int index = PARAM_INDEX.incrementAndGet();
                String newName = "_"+p+"_p"+index;
                for (SimpleName n: method.findAll(SimpleName.class)) {
                    if (p.equals(n.asString())) {
                        n.replace(new SimpleName(newName));
                    }
                }
            }
        }

        System.out.println(":: ::");
        for (CallDash dash: graph.values()) {
            if ( ! dash.isEndDash())
                System.out.println(dash);
        }

        Queue<CallDash> dashQueue = new LinkedList<>();
        Deque<CallDash> callStack = new LinkedList<>();
        Set<String> shifted = new HashSet<>();
        for (CallDash dash: graph.values()) {
            dashQueue.offer(dash);
            while ( ! dashQueue.isEmpty()) {
                CallDash d = dashQueue.poll();
                if (d.isEndDash()) continue;
                callStack.push(d);
                for (String sig: d.getCalleeSignatures()) {
                    CallDash calleeDash = graph.get(sig);
                    dashQueue.offer(calleeDash);
                }
            }
            System.out.println("########################################");
            int i = callStack.size() + 1;
            while ( ! callStack.isEmpty()) {
                --i;
                CallDash d = callStack.pop();
                System.out.println("shift: " + pre(i) + d);
                String dSig = d.getCallerSignature();
                if (shifted.contains(dSig)) {
                    System.out.println("ignored!");
                    continue;
                }
                shifted.add(dSig);
                MethodDeclaration caller = d.getCaller();
                BlockStmt callerBody = caller.getBody().get();
                int len = d.getCallees().length;
                for (int j = 0; j < len; j++) {
                    MethodDeclaration callee = d.getCallees()[j];
                    MethodCallExpr connector = d.getConnectors()[j];
                    if (callee.getType() instanceof VoidType) {
                        List<Statement> calleeBody = new LinkedList<>();
                        //param mapping
                        Iterator<Expression> args = connector.getArguments().iterator();
                        for (Parameter p: callee.getParameters()) {
                            VariableDeclarator vard = new VariableDeclarator(p.getType(), p.getName(), args.next());
                            VariableDeclarationExpr vare = new VariableDeclarationExpr(vard);
                            calleeBody.add( new ExpressionStmt(vare) );
                        }
                        calleeBody.addAll(callee.getBody().get().getStatements());
                        ExpressionStmt connectorStm = ReaderUtil.findClosestParent(connector, ExpressionStmt.class);
                        NodeList<Statement> newCallerBody = new NodeList<>();
                        for (Statement stm: caller.getBody().get().getStatements()) {
                            if (stm == connectorStm) {
                                newCallerBody.addAll(calleeBody);
                            } else {
                                newCallerBody.add(stm);
                            }
                        }
                        callerBody.setStatements(newCallerBody);
                    } else {
                    }
                }
            }
        }
        return rawClass;
    }

    public static String pre(int l) {
        StringBuilder sb = new StringBuilder(l*4);
        for (int i = 0; i < l; i++) {
            sb.append("___ ");
        }
        return sb.toString();
    }
}

