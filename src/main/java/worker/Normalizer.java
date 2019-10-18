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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.Modifier;

public class Normalizer {

    private static final Pattern VERSIONED_NAME = Pattern.compile("_v[0-9]+$");
    private int index = 1;
    
    public ClassOrInterfaceDeclaration normalize(ClassOrInterfaceDeclaration rawClass) {
        List<SimpleName> conflictedName = new LinkedList<>();
        for (SimpleName n: rawClass.findAll(SimpleName.class)) {
            String nname = n.asString();
            if (nname.equals("result")) {
                conflictedName.add(n);
            }
        }
        for (SimpleName n: conflictedName) {
            n.replace( new SimpleName("resultOf") );
        }
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
                if ( call.getScope().isPresent() ) continue;
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

            // return pre-cut
            if (method.getType() instanceof VoidType) continue;
            List<ReturnStmt> currentReturnStms = method.findAll(ReturnStmt.class);
            if (currentReturnStms.size() < 2) continue;
            List<ReturnStmt> returnStms = new LinkedList<>();
            for (ReturnStmt rstm: currentReturnStms) {
                returnStms.add(rstm);
            }
            String outputName = "_output_o"+ index++;
            NameExpr outputVar = new NameExpr(outputName);
            for (ReturnStmt rstm: returnStms) {
                AssignExpr replacement = new AssignExpr(outputVar, rstm.getExpression().get(), AssignExpr.Operator.ASSIGN);
                rstm.replace(new ExpressionStmt(replacement));
            }
            BlockStmt methodBody = method.getBody().get();
            NodeList<Statement> newMethodBody = new NodeList<>();
            newMethodBody.add( createVariableCreationStmt(method.getType(), new SimpleName(outputName), null) );
            newMethodBody.addAll( methodBody.getStatements() );
            newMethodBody.add( new ReturnStmt( outputVar ) );
            methodBody.setStatements(newMethodBody);
        }

        Queue<CallDash> dashQueue = new LinkedList<>();
        Deque<CallDash> callStack = new LinkedList<>();
        Set<String> shifted = new HashSet<>();
        for (CallDash dash: graph.values()) {
            dashQueue.offer(dash);
            while ( ! dashQueue.isEmpty()) {
                CallDash d = dashQueue.poll();
                //System.out.println("in " + d.getCallerSignature() + " vs: " + rawClass.getName().asString());
                if (d.isEndDash()) continue;
                callStack.push(d);
                for (String sig: d.getCalleeSignatures()) {
                    CallDash calleeDash = graph.get(sig);
                    dashQueue.offer(calleeDash);
                }
            }
            int i = callStack.size() + 1;
            while ( ! callStack.isEmpty()) {
                --i;
                CallDash d = callStack.pop();
                String dSig = d.getCallerSignature();
                if (shifted.contains(dSig)) continue;
                shifted.add(dSig);
                MethodDeclaration caller = d.getCaller();
                //BlockStmt callerBody = caller.getBody().get();
                int len = d.getCallees().length;
                for (int j = 0; j < len; j++) {
                    MethodDeclaration callee = d.getCallees()[j].clone();
                    callee = rename(callee);
                    MethodCallExpr connector = d.getConnectors()[j];
                    BlockStmt callerBlock = ReaderUtil.findClosestParent(connector, BlockStmt.class);
                    if (callee.getType() instanceof VoidType) {
                        List<Statement> calleeBody = new LinkedList<>();
                        Iterator<Expression> args = connector.getArguments().iterator();
                        for (Parameter p: callee.getParameters()) {
                            calleeBody.add( createVariableCreationStmt(p.getType(), p.getName(), args.next()) );
                        }
                        calleeBody.addAll(callee.getBody().get().getStatements());
                        ExpressionStmt connectorStm = ReaderUtil.findClosestParent(connector, ExpressionStmt.class);
                        NodeList<Statement> newCallerBody = new NodeList<>();
                        for (Statement stm: callerBlock.getStatements()) {
                            if (stm == connectorStm) {
                                for (Statement cstm: calleeBody) {
                                    newCallerBody.add(cstm.clone());
                                }
                            } else {
                                newCallerBody.add(stm);
                            }
                        }
                        callerBlock.setStatements(newCallerBody);
                    } else {
                        List<Statement> newCalleeBody = new LinkedList<>();
                        Iterator<Expression> args = connector.getArguments().iterator();
                        for (Parameter p: callee.getParameters()) {
                            newCalleeBody.add( createVariableCreationStmt(p.getType(), p.getName(), args.next()) );
                        }
                        NodeList<Statement> calleeStms = callee.getBody().get().getStatements();
                        int lastStmIndex = calleeStms.size() - 1;
                        int counter = 0;
                        for (Statement calleeStm: calleeStms) {
                            if (counter != lastStmIndex) { // assume return statement is the last one
                                newCalleeBody.add(calleeStm);
                            }
                            ++counter;
                        }
                        ExpressionStmt connectorStm = ReaderUtil.findClosestParent(connector, ExpressionStmt.class);
                        Node parentOfConnector = connector.getParentNode().get();
                        boolean isLonelyCall = false;
                        if (parentOfConnector == connectorStm) {
                            isLonelyCall = true;
                        }
                        NodeList<Statement> newCallerBody = new NodeList<>();
                        for (Statement stm: callerBlock.getStatements()) {
                            if (stm == connectorStm) {
                                for (Statement cstm: newCalleeBody) {
                                    newCallerBody.add(cstm.clone());
                                }
                                if (isLonelyCall) continue;
                            }
                            newCallerBody.add(stm);
                        }
                        callerBlock.setStatements(newCallerBody);
                        if ( ! isLonelyCall) {
                            Expression connectorReplacement = callee.findFirst(ReturnStmt.class).get().getExpression().get();
                            connector.replace(connectorReplacement);
                        }
                    }
                }
            }
        }
        removePrivateFunction(rawClass);
        return rawClass;
    }

    private Statement createVariableCreationStmt(Type type, SimpleName name, Expression init) {
        VariableDeclarator vard = new VariableDeclarator(type, name, init);
        VariableDeclarationExpr vare = new VariableDeclarationExpr(vard);
        return new ExpressionStmt(vare);
    }

    private void removePrivateFunction(ClassOrInterfaceDeclaration rawClass) {
        List<Node> useless = new LinkedList<>();
        for (BodyDeclaration<?> mem: rawClass.getMembers()) {
            if (mem instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) mem;
                for (Modifier mod: method.getModifiers()) {
                    if (mod.getKeyword() == Modifier.Keyword.PRIVATE) {
                        useless.add(mem);
                        break;
                    }
                }
            }
        }
        for (Node n: useless) n.remove();
    }

    private MethodDeclaration rename(MethodDeclaration inmethod) {
        Set<String> candi = new HashSet<>();
        for (VariableDeclarator vari: inmethod.findAll(VariableDeclarator.class)) {
            candi.add( vari.getName().asString() );
        }
        for (Parameter p: inmethod.getParameters()) {
            candi.add( p.getName().asString() );
        }
        List<SimpleName> targets = new LinkedList<>();
        for (SimpleName n: inmethod.findAll(SimpleName.class)) {
            Node parent = n.getParentNode().get();
            if ( parent instanceof MethodCallExpr ) continue;
            if ( ! candi.contains( n.asString() ) ) continue;
            targets.add( n );
        }
        int ci = index++;
        for (SimpleName n: targets) {
            String name = n.asString();
            if (VERSIONED_NAME.matcher(name).find()) {
                name = name.substring(0, name.lastIndexOf("_"));
            } else {
            }
            String newName = name + "_v" + ci;
            n.replace(new SimpleName(newName));
        }
        return inmethod;
    }
}

