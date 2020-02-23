package worker;

import mw.ConstantMiddleware;

import static meta.Name.*;
import meta.*;
import reader.*;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.google.common.collect.ImmutableList;

public class MethodWorker {

    private static final ImmutableList<MockingReader> MOCK_READER = ImmutableList.of(
            new ReturnMockReader(),
            new VoidMockReader(),
            new StaticVoidMockReader(),
            new FunctionThrowMockReader(),
            new NewInstanceMockReader(),

            new IndirectStaticVoidMockReader(),
            new IndirectStaticReturnMockReader(),
            new PrivateReturnMockReader()
            );
    private static final ImmutableList<MockingReader> VERIFY_READER = ImmutableList.of(
            new InvocationVerifyReader(),
            new StaticInvocationVerifyReader()
            );

    private MethodDeclaration methodUnit;
    private ClassWorker classWorker;
    private MockingMeta records = new MockingMeta();
    private MockingMeta rechecks = new MockingMeta();
    private VarPool cooked = new VarPool();
    private Set<String> takenNames = new HashSet<>();
    private Set<String> ics = new HashSet<>();
    private Set<String> usedNames = new HashSet<>();
    private Map<String, String> vars = new HashMap<>();

    public MethodWorker(MethodDeclaration mu) {
        methodUnit = mu;
        WoodLog.reachMethod(methodUnit.getName().asString());
        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            vars.putIfAbsent(vari.getName().asString(), vari.getType().asString());
        }
    }

    public MethodWorker setClassWorker(ClassWorker cl) {
        classWorker = cl;
        return this;
    }

    public MethodWorker setTakenNames(Set<String> tn) {
        takenNames.addAll(tn);
        return this;
    }

    public MethodWorker setICs(Set<String> classLevelICs) {
        ics.addAll(classLevelICs);
        List<Node> useless = new ArrayList<>();
        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            if ("InvocationCounter".equals(vari.getType().asString())) {
                ics.add(vari.getName().asString());
                useless.add(vari);
            }
        }
        for (Node u: useless) {
            u.remove();
        }
        return this;
    }

    public void transform() {
        normalizeICs();

        replaceInstanceMockDeclaration(methodUnit);
        Set<String> typeMockedAsStatic = removeStaticMockDeclaration(methodUnit);
        recordMockForStaticHijacking(typeMockedAsStatic);

        Statement[] baseStms = getStms(methodUnit);
        int len = baseStms.length;
        int[] stmTypes = new int[len];
        for (int i = 0; i < len; i++) {
            Node n = baseStms[i];
            Node belowNode = null;
            if (i+1 < baseStms.length) {
                belowNode = baseStms[i+1];
            }
            int nType = checkType(n, belowNode);
            if (nType == FOLLOWED_MOCK_STM) {
                stmTypes[i] = MOCK_STM;
                i++;
                stmTypes[i] = MOCK_STM;
            } else if(nType == FOLLOWED_VERIFY_STM) {
                stmTypes[i] = VERIFY_STM;
                i++;
                stmTypes[i] = VERIFY_STM;
            } else {
                stmTypes[i] = nType;
            }
        }
        if ( ! rechecks.isEmpty()) {
            int lastVerifyStm = baseStms.length - 1;
            while (lastVerifyStm >= 0 && stmTypes[lastVerifyStm] != VERIFY_STM) {
                WoodLog.loopLog(this, 112);
                lastVerifyStm--;
            }
            if (lastVerifyStm >= 0) {
                Statement verifications = VerifyWorker.forWorker(this).transform(rechecks);
                baseStms[lastVerifyStm].replace(verifications);
            }
        }

        if ( ! records.isEmpty()) {
            int lastMockStm = baseStms.length - 1;
            while (lastMockStm >= 0 && stmTypes[lastMockStm] != MOCK_STM) {
                WoodLog.loopLog(this, 122);
                lastMockStm--;
            }
            if (lastMockStm >= 0) {
                Statement[] expectations = Caster.forWorker(this).replay(records);
                Statement pointOfRep = baseStms[lastMockStm];
                BlockStmt block = ReaderUtil.findClosestParent(pointOfRep, BlockStmt.class);
                Iterator<Statement> lineIte = block.getStatements().iterator();
                NodeList<Statement> newBlockStmts = new NodeList<>();
                Statement line = null;
                while(lineIte.hasNext()) {
                    WoodLog.loopLog(this, 131);
                    line = lineIte.next();
                    if (line == pointOfRep) break;
                    newBlockStmts.add(line);
                }
                if (line == null) {
                    System.out.println("Found no replacement point");
                } else {
                    for(Statement expectation: expectations) {
                        newBlockStmts.add(expectation);
                    }
                }
                while(lineIte.hasNext()) {
                    WoodLog.loopLog(this, 143);
                    newBlockStmts.add(lineIte.next());
                }
                block.setStatements(newBlockStmts);
            }
        }

        for (int i = 0; i < len; ++i) {
            if (stmTypes[i] != NORMAL_STM && stmTypes[i] != UNKNOW_STM) {
                baseStms[i].remove();
            }
        }
    }

    private void normalizeICs() {
        for (MethodCallExpr mce: methodUnit.findAll(MethodCallExpr.class)) {
            if ("times".equals( mce.getName().asString() )) {
                mce.getScope().ifPresent(callee -> {
                    if (ics.contains(callee.toString())) {
                        Node stmNode = ReaderUtil.findClosestParent(mce, ExpressionStmt.class);
                        String assertStm = stmNode.toString();
                        int time = ReaderUtil.getICExpectedTimes(assertStm);
                        if (time < 0) {
                            WoodLog.attach(WARNING, "Cannot extract time from statement: " + assertStm);
                        } else {
                            Node blockNode = ReaderUtil.findClosestParent(stmNode, BlockStmt.class);
                            List<Node> sibs = blockNode.getChildNodes();
                            int i = sibs.size() - 1;
                            while (i >= 0 && sibs.get(i) != stmNode) {
                                WoodLog.loopLog(this, 171);
                                --i;
                            }
                            boolean match = false;
                            Node nameNode = null;
                            while ( i > 0 && ! match) {
                                WoodLog.loopLog(this, 174);
                                --i;
                                for (NameExpr name: sibs.get(i).findAll(NameExpr.class)) {
                                    if (callee.toString().equals(name.getNameAsString())) {
                                        nameNode = name;
                                        match = true;
                                        break;
                                    }
                                }
                            }
                            if ( i < 0) {
                                WoodLog.attach(WARNING, "Found no ic instance setup for: " + assertStm);
                            } else {
                                nameNode.replace(new NameExpr("times("+time+")"));
                                stmNode.remove();
                            }
                        }
                    }
                });
            }
        }
    }

    private void replaceInstanceMockDeclaration(MethodDeclaration methodUnit) {
        List<Entry<Node, Node>> replacementPair = new LinkedList<>();
        List<Node> useless = new LinkedList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                Expression firstArgumentExp = call.getArguments().get(0);
                if ( ! (firstArgumentExp instanceof ClassExpr)) {
                    WoodLog.attach(WARNING, "Found non-class argument for mock call: " + call.toString());
                    continue;
                }
                String type = ((ClassExpr)firstArgumentExp).getType().asString();
                String suggestionName = NameUtil.createTypeBasedName(type, takenNames);
                String replacementName = classWorker.recordAsInstanceMocked(type, suggestionName, usedNames);

                Node parent = call.getParentNode().get();
                if (parent instanceof VariableDeclarator || parent instanceof AssignExpr) {
                    String oriName = "";
                    if (parent instanceof VariableDeclarator) {
                        oriName = ((VariableDeclarator)parent).getName().asString();
                    } else {
                        oriName = ((NameExpr)((AssignExpr)parent).getTarget()).getName().asString();
                    }

                    Statement callStm = ReaderUtil.findClosestParent(parent, ExpressionStmt.class);
                    BlockStmt callBlock = ReaderUtil.findClosestParent(parent, BlockStmt.class);
                    Iterator<Statement> line = callBlock.getStatements().iterator();
                    while (line.next() != callStm) {
                        WoodLog.loopLog(this, 222);
                    }
                    while (line.hasNext()) {
                        WoodLog.loopLog(this, 224);
                        Statement currentLine = line.next();
                        for (SimpleName fellowName: currentLine.findAll(SimpleName.class)) {
                            if ((fellowName.getParentNode().get() instanceof NameExpr) && fellowName.asString().equals( oriName )) {
                                replacementPair.add(new SimpleEntry<>(fellowName, new SimpleName(replacementName)));
                            }
                        }
                    }
                    useless.add(callStm);
                } else {
                    replacementPair.add(new SimpleEntry<>(call, new NameExpr(replacementName)));
                }

                takenNames.add(replacementName);
                usedNames.add(type+":"+replacementName);
                cooked.addVar(replacementName).underType(type).from(MOCKED_INSTANCE);
            }
        }
        for (Entry<Node, Node> p: replacementPair) {
            p.getKey().replace(p.getValue());
        }
        for (Node u: useless) u.remove();
    }

    private Set<String> removeStaticMockDeclaration(MethodDeclaration methodUnit) {
        Set<String> staticMocked = new HashSet<>();
        LinkedList<Node> useless = new LinkedList<>();
        BlockStmt methodBody = methodUnit.getBody().get();
        for (Statement stmt: methodBody.getStatements()) {
            for (MethodCallExpr call: stmt.findAll(MethodCallExpr.class)) {
                if ("mockStatic".equals(call.getName().asString())) {
                    for (Expression ex: call.getArguments()) {
                        if ( ! (ex instanceof ClassExpr)) {
                            WoodLog.attach(WARNING, "Found non-class argument for mockStatic call: " + call.toString());
                            continue;
                        }
                        String type = ((ClassExpr)ex).getType().asString();
                        if ( ! isCookedType(type)) {
                            /*
                            String name = NameUtil.createTypeBasedName(type, takenNames);
                            if ( classWorker.recordAsTypeMocked(type, name) ) {
                                takenNames.add(name);
                            }
                            cooked.addVar(name).underType(type).from(STATIC_INVOCATION);
                            */
                            staticMocked.add(type);
                        }
                    }
                    useless.push(stmt);
                }
            }
        }
        while ( ! useless.isEmpty()) {
            WoodLog.loopLog(this, 276);
            methodBody.remove(useless.pop());
        }
        return staticMocked;
    }

    private void recordMockForStaticHijacking(Set<String> staticMock) {
        for (String type: staticMock) {
            String suggestName = NameUtil.createTypeBasedName(type, takenNames);
            if ( classWorker.recordAsTypeMocked(type, suggestName) ) {
                takenNames.add(suggestName);
            }
        }
    }

    private static Statement[] getStms(MethodDeclaration methodUnit) {
        List<Statement> stms = new LinkedList<>();
        for(Statement stm: methodUnit.getBody().get().getStatements()) {
            List<ExpressionStmt> child = stm.findAll(ExpressionStmt.class);
            if (child.isEmpty()) {
                stms.add(stm);
            } else {
                stms.addAll(child);
            }
        }
        return stms.toArray(new Statement[stms.size()]);
    }

    private int checkType(Node node, Node belowNode) {
        String nodeAsString = ConstantMiddleware.i().hijack( node.toString() );
        for (MockingReader reader: MOCK_READER) {
            StatementPiece stmp = reader.read(nodeAsString, node, belowNode);
            int stmType = stmp.getType();
            if (stmType != UNKNOW_STM) {
                Craft craft = stmp.getCraft();
                if (craft != null) {
                    records.getBySubjectName( craft.getSubjectName() )
                        .getByMethodName( craft.getMethodName() )
                        .add( craft.getCallMeta() );
                }
                String newMockType = stmp.getRequestAsMock();
                int rType = stmp.getRawType();
                if (newMockType != null && rType == NEW_INSTANT_INJECTION) {
                    // TODO: better handle for whenNew injection
                }
                return stmType;
            }
        }
        for (MockingReader reader: VERIFY_READER) {
            StatementPiece stmp = reader.read(nodeAsString, node, belowNode);
            int stmType = stmp.getType();
            if (stmType != UNKNOW_STM) {
                Craft craft = stmp.getCraft();
                if (craft != null) {
                    rechecks.getBySubjectName( craft.getSubjectName() )
                        .getByMethodName( craft.getMethodName() )
                        .add( craft.getCallMeta() );
                }
                return stmType;
            }
        }
        return NORMAL_STM;
    }

    private boolean isCookedType(String type) {
        return cooked.typeInPool(type);
    }

    /**
     * Don't pass type with package in
     */
    public String[] findType(String subject) {
        char fc = subject.charAt(0);
        if ('A' <= fc && fc <= 'Z') {
            return classWorker.findTypeByName(subject);
        } else {
            String[] type = null;
            if (type == null)
                type = classWorker.findTypeByOwner(subject);
            return type;
        }
    }

    public String findTypeWithoutPackage(String subject) {
        String type = vars.get(subject); // method scope search
        if (type == null) {
            //WoodLog.attach("Found no funtion level type of ["+subject+"] !");
            type = classWorker.findTypeWithoutPackage(subject); // class scope search
        }
        return type;
    }

    public void addImportationIfAbsent(String im) {
        classWorker.addImportationIfAbsent(im);
    }
}
