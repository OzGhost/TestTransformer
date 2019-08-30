package worker;

import static meta.Name.*;
import meta.*;
import reader.*;
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
            new PrivateStaticReturnMockReader()
            );
    private static final ImmutableList<MockingReader> VERIFY_READER = ImmutableList.of(
            new InvocationVerifyReader(),
            new StaticInvocationVerifyReader()
            );

    private MethodDeclaration methodUnit;
    private ClassWorker classWorker;
    private MockingMeta records = new MockingMeta();
    private MockingMeta rechecks = new MockingMeta();
    private List<String[]> cooked = new ArrayList<>();
    private List<String[]> declared = new ArrayList<>();
    private Set<String> takenNames = new HashSet<>();
    private Set<String> icNames = new HashSet<>();


    public MethodWorker(MethodDeclaration mu) {
        methodUnit = mu;
    }

    public MethodWorker setClassWorker(ClassWorker cl) {
        classWorker = cl;
        return this;
    }

    public MethodWorker setRequiredFields(List<VariableDeclarator> rfs) {
        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            takenNames.add( vari.getName().asString() );
            declared.add(new String[]{
                vari.getType().asString(),
                vari.getName().asString()
            });
        }
        Set<String> calledName = new HashSet<>();
        for (NameExpr ne: methodUnit.findAll(NameExpr.class)) {
            calledName.add(ne.getName().asString());
        }
        for (VariableDeclarator vari: rfs) {
            String name = vari.getName().asString();
            if ( calledName.contains(name) && ! takenNames.contains(name)) {
                String type = vari.getType().asString();
                cooked.add(new String[]{type, name});
                takenNames.add(name);
            }
        }
        return this;
    }

    public MethodWorker setICNames(List<String> icns) {
        icNames.addAll(icns);
        List<Node> useless = new ArrayList<>();
        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            if ("InvocationCounter".equals(vari.getType().asString())) {
                icNames.add(vari.getName().asString());
                useless.add(vari);
            }
        }
        for (Node u: useless) {
            u.remove();
        }
        for (MethodCallExpr mce: methodUnit.findAll(MethodCallExpr.class)) {
            if ("times".equals( mce.getName().asString() )) {
                mce.getScope().ifPresent(callee -> {
                    if (icNames.contains(callee.toString())) {
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
        return this;
    }

    public void transform() {
        WoodLog.reachMethod(methodUnit.getName().asString());

        replaceInstanceMockDeclaration(methodUnit);
        replaceStaticMockDeclaration(methodUnit);

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
        methodUnit.setParameters( createParameters() );
    }

    private void replaceInstanceMockDeclaration(MethodDeclaration methodUnit) {
        List<Node> ori = new ArrayList<>();
        List<Node> repl = new ArrayList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                Expression firstArgumentExp = call.getArguments().get(0);
                if ( ! (firstArgumentExp instanceof ClassExpr)) {
                    WoodLog.attach(WARNING, "Found non-class argument for mock call: " + call.toString());
                    continue;
                }
                Type mockingType = ((ClassExpr)firstArgumentExp).getType();
                String type = mockingType.asString();
                String rname = NameUtil.createTypeBasedName(type, takenNames);

                ori.add(call);
                repl.add(new NameExpr(rname));

                cooked.add(new String[]{type, rname});
                takenNames.add(rname);
            }
        }
        for (int i = 0; i < ori.size(); ++i) {
            ori.get(i).replace(repl.get(i));
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
                        Type argType = ((ClassExpr)ex).getType();
                        String type = argType.asString();
                        if ( ! isCookedType(type)) {
                            String name = NameUtil.createTypeBasedName(type, takenNames);
                            cooked.add(new String[]{type, name});
                            takenNames.add(name);
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
            int stmType = reader.read(nodeAsString, node, belowNode);
            if (stmType != UNKNOW_STM) {
                Craft craft = reader.getCraft();
                records.getBySubjectName( craft.getSubjectName() )
                    .getByMethodName( craft.getMethodName() )
                    .add( craft.getCallMeta() );
                return stmType;
            }
        }
        for (MockingReader reader: VERIFY_READER) {
            int stmType = reader.read(nodeAsString, node, belowNode);
            if (stmType != UNKNOW_STM) {
                Craft craft = reader.getCraft();
                rechecks.getBySubjectName( craft.getSubjectName() )
                    .getByMethodName( craft.getMethodName() )
                    .add( craft.getCallMeta() );
                return stmType;
            }
        }
        return NORMAL_STM;
    }

    private NodeList<Parameter> createParameters() {
        NodeList<Parameter> output = new NodeList<>();
        int len = cooked.size();
        for (int i = 0; i < len; ++i) {
            String[] ci = cooked.get(i);
            String type = ci[0];
            String name = ci[1];
            Parameter param = new Parameter(new ClassOrInterfaceType(type), name);
            param.setAnnotations(new NodeList<>(new MarkerAnnotationExpr("Mocked")));
            output.add(param);
        }
        return output;
    }

    private boolean isCookedType(String type) {
        int len = cooked.size();
        for (int i = 0; i < len; ++i) {
            if (type.equals(cooked.get(i)[0])) {
                return true;
            }
        }
        return false;
    }

    public String[] findType(String subject) {
        int len = cooked.size();
        for (int i = 0; i < len; ++i) {
            String[] currentPair = cooked.get( i );
            String type = currentPair[0];
            String name = currentPair[1];
            if (type.equals(subject) || name.equals(subject)) {
                return classWorker.findType(type);
            }
        }
        len = declared.size();
        for (int i = 0; i < len; ++i) {
            String[] cp = declared.get(i);
            String type = cp[0];
            String name = cp[1];
            if (type.equals(subject) || name.equals(subject)) {
                return classWorker.findType(type);
            }
        }
        char fc = subject.charAt(0);
        if ('A' <= fc && fc <= 'Z') {
            return classWorker.findType(subject);
        }
        WoodLog.attach(ERROR, "Cannot find type of '" + subject + "' !");
        return new String[0];
    }

    public void addImportationIfAbsent(String im) {
        classWorker.addImportationIfAbsent(im);
    }
}
