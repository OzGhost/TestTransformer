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
            new StaticVoidMockReader()
            );
    private static final ImmutableList<MockingReader> VERIFY_READER = ImmutableList.of(
            new InvocationVerifyReader(),
            new StaticInvocationVerifyReader()
            );

    private MethodDeclaration methodUnit;
    private MockingMeta records = new MockingMeta();
    private MockingMeta rechecks = new MockingMeta();
    private List<String[]> cooked = new ArrayList<>();
    private Set<String> takenNames = new HashSet<>();


    public MethodWorker(MethodDeclaration mu) {
        methodUnit = mu;
    }

    public MethodWorker setRequiredFields(List<VariableDeclarator> rfs) {
        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            takenNames.add( vari.getName().asString() );
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

    public void transform() {
        WoodLog.reachMethod(methodUnit.getName().asString());

        replaceInstanceMockDeclaration(methodUnit);
        replaceStaticMockDeclaration(methodUnit);

        Statement[] baseStms = getStms(methodUnit);
        int[] stmTypes = new int[baseStms.length];
        for (int i = 0; i < baseStms.length; i++) {
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
        int mockBreakPoint = baseStms.length - 1;
        while (mockBreakPoint >= 0 && stmTypes[mockBreakPoint] != MOCK_STM) {
            mockBreakPoint--;
        }
        mockBreakPoint++;
        NodeList<Statement> newBodyStmts = new NodeList<>();

        for (int i = 0; i < mockBreakPoint; i++) {
            if (stmTypes[i] == NORMAL_STM) {
                newBodyStmts.add( baseStms[i] );
            }
        }

        Statement expectations = MockWorker.transform(records);
        newBodyStmts.add(expectations);

        for (int i = mockBreakPoint; i < baseStms.length; i++) {
            if (stmTypes[i] == NORMAL_STM) {
                newBodyStmts.add( baseStms[i] );
            }
        }

        Statement verifications = VerifyWorker.transform(rechecks);
        newBodyStmts.add(verifications);

        methodUnit.getBody().get().setStatements(newBodyStmts);

        //System.out.println(records);
        //System.out.println(rechecks);
        //WoodLog.printCuts();
        methodUnit.setParameters( createParameters() );
    }

    private void replaceInstanceMockDeclaration(MethodDeclaration methodUnit) {
        List<Node> ori = new ArrayList<>();
        List<Node> repl = new ArrayList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                Type mockingType = ((ClassExpr)call.getArguments().get(0)).getType();
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
        for(Statement n: methodUnit.getBody().get().getStatements()) {
            stms.add(n);
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
}
