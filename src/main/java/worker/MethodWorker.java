package worker;

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
            new PrivateStaticVoidMockReader(),
            new PrivateStaticReturnMockReader(),
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
    private VarPool declared = new VarPool();
    private Set<String> takenNames = new HashSet<>();
    private Set<String> partialMockVars = new HashSet<>();
    private Set<String> ics = new HashSet<>();
    private Set<String> usedNames = new HashSet<>();

    public MethodWorker(MethodDeclaration mu) {
        methodUnit = mu;
        WoodLog.reachMethod(methodUnit.getName().asString());
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
        replaceStaticMockDeclaration(methodUnit);
        replaceSpy();

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
            while (lastVerifyStm >= 0 && stmTypes[lastVerifyStm] != VERIFY_STM)
                lastVerifyStm--;
            if (lastVerifyStm >= 0) {
                Statement verifications = VerifyWorker.forWorker(this).transform(rechecks);
                baseStms[lastVerifyStm].replace(verifications);
            }
        }

        if ( ! records.isEmpty()) {
            reduceUnnecessaryPartialMock();
            int lastMockStm = baseStms.length - 1;
            while (lastMockStm >= 0 && stmTypes[lastMockStm] != MOCK_STM)
                lastMockStm--;
            if (lastMockStm >= 0) {
                Statement expectations = Caster.forWorker(this).replay(records);
                baseStms[lastMockStm].replace(expectations);
            }
        }
        for (int i = 0; i < len; ++i) {
            if (stmTypes[i] != NORMAL_STM) {
                baseStms[i].remove();
            }
        }
        //methodUnit.getParameters().addAll( createParameters() );
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
                            while (i >= 0 && sibs.get(i) != stmNode) --i;
                            boolean match = false;
                            Node nameNode = null;
                            while ( i > 0 && ! match) {
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

                replacementPair.add(new SimpleEntry<>(call, new NameExpr(replacementName)));
                takenNames.add(replacementName);
                usedNames.add(type+":"+replacementName);
                cooked.addVar(replacementName).underType(type).from(MOCKED_INSTANCE);
            }
        }
        for (Entry<Node, Node> p: replacementPair) {
            p.getKey().replace(p.getValue());
        }
    }

    private void replaceStaticMockDeclaration(MethodDeclaration methodUnit) {
        List<Type> staticMocks = new ArrayList<>();
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
                            String name = NameUtil.createTypeBasedName(type, takenNames);
                            if ( classWorker.recordAsTypeMocked(type, name) ) {
                                takenNames.add(name);
                            }
                            cooked.addVar(name).underType(type).from(STATIC_INVOCATION);
                        }
                    }
                    useless.push(stmt);
                }
            }
        }
        while ( ! useless.isEmpty()) {
            methodBody.remove(useless.pop());
        }
    }

    private void replaceSpy() {
        List<MethodCallExpr> spyCalls = new LinkedList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("spy".equals(call.getName().asString())) {
                Optional<Node> parentNodeOp = call.getParentNode();
                if ( ! parentNodeOp.isPresent()) {
                    WoodLog.attach(ERROR, "Find spy call out-of-context");
                    continue;
                }
                Node parentNode = parentNodeOp.get();
                String spyVar = "";
                if (parentNode instanceof VariableDeclarator) {
                    spyVar = ((VariableDeclarator)parentNode).getName().asString();
                } else if (parentNode instanceof AssignExpr) {
                    Expression tExpr = ((AssignExpr)parentNode).getTarget();
                    if (tExpr instanceof NameExpr) {
                        spyVar = ((NameExpr)tExpr).getName().asString();
                    } else {
                        WoodLog.attach(ERROR, "Find spy assign to sth not variable");
                        continue;
                    }
                } else {
                    WoodLog.attach(ERROR, "Find spy in unsupported context: " + parentNode.getClass().getCanonicalName());
                    continue;
                }
                spyCalls.add(call);
                partialMockVars.add(spyVar);
            }
        }
        for (MethodCallExpr c: spyCalls) {
            NodeList<Expression> args = c.getArguments();
            if (args.size() != 1) {
                WoodLog.attach(ERROR, "Found non-supported number of spy call's args: expected 1 but was " + args.size());
                continue;
            }
            Expression spyInstance = args.get(0);
            if (spyInstance instanceof ClassExpr) {
                WoodLog.attach(WARNING, "Spy a class");
                ClassExpr spyClass = (ClassExpr) spyInstance;
                c.replace( new NameExpr("new "+spyClass.getType().asString()+"()") );
            } else {
                c.replace(spyInstance);
            }
        }
    }

    private static Statement[] getStms(MethodDeclaration methodUnit) {
        ArrayList<Statement> stms = new ArrayList<>();
        for(Statement stm: methodUnit.findAll(ExpressionStmt.class)) {
            stms.add(stm);
        }
        return stms.toArray(new Statement[stms.size()]);
    }

    private int checkType(Node node, Node belowNode) {
        String nodeAsString = node.toString();
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
                String newMock = stmp.getRequestAsMock();
                int rType = stmp.getRawType();
                if (newMock != null && rType == NEW_INSTANT_INJECTION) {
                    if ( ! cooked.typeInPool(newMock)) {
                        //TODO: better handle for new instance injection (if possible)
                        WoodLog.attach(WARNING, "The given type suppose to be mocked but not: " + newMock);
                        String newMockVarName = NameUtil.createTypeBasedName(newMock, takenNames);
                        cooked.addVar(newMockVarName).underType(newMock).from(NEW_OPERATION_INVOCATION);
                    }
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

    private void reduceUnnecessaryPartialMock() {
        Set<String> requiredPM = new HashSet<>();
        for (String pmv: partialMockVars) {
            if ( records.containsSubject(pmv) ) {
                String[] ti = findType(pmv);
                if (ti.length != 2) continue;
                String type = ti[0];
                List<VarPiece> sameTypeCooked = cooked.findAllByType(type);
                boolean accidentlyMocked = false;
                List<VarPiece> uselessDish = new LinkedList<>();
                for (VarPiece v: sameTypeCooked) {
                    if (v.getSource() == STATIC_INVOCATION) {
                        uselessDish.add(v);
                    } else {
                        accidentlyMocked = true;
                        break;
                    }
                }
                if (accidentlyMocked) {
                    WoodLog.attach(ERROR, "Suppose to be mock partialy but fully mocked: " + type);
                } else {
                    for (VarPiece u: uselessDish) {
                        cooked.remove(u);
                    }
                    requiredPM.add(pmv);
                }
            }
        }
        partialMockVars = requiredPM;
    }

    private NodeList<Parameter> createParameters() {
        NodeList<Parameter> output = new NodeList<>();
        for (VarPiece v: cooked) {
            String type = v.getType();
            String name = v.getName();
            Parameter param = new Parameter(new ClassOrInterfaceType(null, type), name);
            param.setAnnotations(new NodeList<>(new MarkerAnnotationExpr("Mocked")));
            output.add(param);
        }
        return output;
    }

    private boolean isCookedType(String type) {
        return cooked.typeInPool(type);
    }

    public String[] findType(String subject) {
        VarPiece v = cooked.find(subject);
        if (v == null)
            v = declared.find(subject);
        if (v == null) {
            char fc = subject.charAt(0);
            if ('A' <= fc && fc <= 'Z') {
                return classWorker.findType(subject);
            }
        } else {
            return classWorker.findType(v.getType());
        }
        WoodLog.attach(ERROR, "Cannot find type of <" + subject + "> !");
        return new String[0];
    }

    public void addImportationIfAbsent(String im) {
        classWorker.addImportationIfAbsent(im);
    }

    public Set<String> getPMV() {
        return partialMockVars;
    }
}
