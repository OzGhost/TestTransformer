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
    private Set<Integer> touched = new HashSet<>();
    
    public ClassOrInterfaceDeclaration normalize(ClassOrInterfaceDeclaration rawClass) {
        for (MethodDeclaration m: rawClass.findAll(MethodDeclaration.class)) {
            List<SimpleName> rname = new LinkedList<>();
            for (VariableDeclarator n: m.findAll(VariableDeclarator.class)) {
                String nname = n.getName().asString();
                if (nname.equals("result")) {
                    ExpressionStmt parent = ReaderUtil.findClosestParent(n, ExpressionStmt.class);
                    BlockStmt block = ReaderUtil.findClosestParent(parent, BlockStmt.class);
                    Iterator<Statement> blockIte = block.getStatements().iterator();
                    while(blockIte.next() != parent) {
                        WoodLog.loopLog(this, 54);
                    }
                    while (blockIte.hasNext()) {
                        WoodLog.loopLog(this, 55);
                        for (SimpleName rp: blockIte.next().findAll(SimpleName.class)) {
                            if (rp.asString().equals("result"))
                                rname.add(rp);
                        }
                    }
                    rname.add(n.getName());
                }
            }
            for (SimpleName n: rname) {
                n.replace( new SimpleName("mlresult") );
            }
        }
        List<SimpleName> conflictedName = new LinkedList<>();
        for (SimpleName n: rawClass.findAll(SimpleName.class)) {
            String nname = n.asString();
            if (nname.equals("result")) {
                conflictedName.add(n);
            }
        }
        for (SimpleName n: conflictedName) {
            n.replace( new SimpleName("clresult") );
        }
        Map<String, CallDash> graph = new HashMap<>();
        for (BodyDeclaration<?> mem: rawClass.getMembers()) {
            if ( ! (mem instanceof MethodDeclaration) ) continue;
            MethodDeclaration method = (MethodDeclaration) mem;
            //if ( isNeutralFunction(method) ) continue;
            if ( isVarArgedFunction(method) ) continue;
            touched.add(System.identityHashCode(mem));
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
        Set<String> callerSigs = new HashSet<>();
        Set<String> shifted = new HashSet<>();
        for (CallDash dash: graph.values()) {
            dashQueue.offer(dash);
            while ( ! dashQueue.isEmpty()) {
                WoodLog.loopLog(this, 134);
                CallDash d = dashQueue.poll();
                if (d.isEndDash()) {
                    continue;
                }
                callStack.push(d);
                callerSigs.add(d.getCallerSignature());
                for (String sig: d.getCalleeSignatures()) {
                    if (callerSigs.contains(sig)) {
                        WoodLog.attach("CIRCULAR: " + sig + " "  + rawClass.getName().toString());
                    } else {
                        CallDash calleeDash = graph.get(sig);
                        dashQueue.offer(calleeDash);
                    }
                }
            }
            int i = callStack.size() + 1;
            while ( ! callStack.isEmpty()) {
                WoodLog.loopLog(this, 146);
                --i;
                CallDash d = callStack.pop();
                String dSig = d.getCallerSignature();
                if (shifted.contains(dSig)) continue;
                shifted.add(dSig);
                MethodDeclaration caller = d.getCaller();
                //BlockStmt callerBody = caller.getBody().get();
                int len = d.getCallees().length;
                for (int j = len-1; j >= 0; --j) {
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

    private boolean isNeutralFunction(MethodDeclaration method) {
        String asString = method.toString();
        if (asString.contains("when(")) return false;
        if (asString.contains("whenNew(")) return false;
        if (asString.contains("mockStatic(")) return false;
        if (ReaderUtil.hasAnnotation(method, "Test")) return false;
        return true;
    }

    private boolean isVarArgedFunction(MethodDeclaration method) {
        for (Parameter p: method.getParameters()) {
            if (p.isVarArgs()) return true;
        }
        return false;
    }

    private Statement createVariableCreationStmt(Type type, SimpleName name, Expression init) {
        VariableDeclarator vard = new VariableDeclarator(type, name, init);
        VariableDeclarationExpr vare = new VariableDeclarationExpr(vard);
        return new ExpressionStmt(vare);
    }

    private void removePrivateFunction(ClassOrInterfaceDeclaration rawClass) {
        List<Node> useless = new LinkedList<>();
        for (BodyDeclaration<?> mem: rawClass.getMembers()) {
            if ( ! touched.contains( System.identityHashCode(mem) )) continue;
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
            String newName = name + "_v" + ci;
            if (VERSIONED_NAME.matcher(name).find()) {
                //name = name.substring(0, name.lastIndexOf("_"));
                newName = name+ci;
            } 
            n.replace(new SimpleName(newName));
        }
        return inmethod;
    }
}

