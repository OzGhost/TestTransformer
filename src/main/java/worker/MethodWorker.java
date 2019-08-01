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

    private ClassWorker upperLevel;
    private MockingMeta records = new MockingMeta();
    private MockingMeta rechecks = new MockingMeta();
    private List<String[]> cooked = new ArrayList<>();
    private Set<String> takenName = new HashSet<>();
    //private List<VariableDeclarator> requiredFields = new ArrayList<>(0);


    public MethodWorker(ClassWorker ul) {
        upperLevel = ul;
    }

    public MethodWorker setRequiredFields(List<VariableDeclarator> rfs) {
        //requiredFields = rfs;
        for (VariableDeclarator vari: rfs) {
            String varName = vari.getName().asString();
            String varType = vari.getType().asString();
            takenName.add(varName);
            cooked.add(new String[]{varType, varName});
        }
        return this;
    }

    public void transform(MethodDeclaration methodUnit) {
        WoodLog.reachMethod(methodUnit.getName().asString());

        for (VariableDeclarator vari: methodUnit.findAll(VariableDeclarator.class)) {
            String varName = vari.getName().asString();
            takenName.add(varName);
        }

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
        WoodLog.printCuts();
    }

    private void replaceInstanceMockDeclaration(MethodDeclaration methodUnit) {
        List<Node> ori = new ArrayList<>();
        List<Node> repl = new ArrayList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                Type mockingType = ((ClassExpr)call.getArguments().get(0)).getType();
                String replacementName = upperLevel.recordMock(mockingType.toString());

                ori.add(call);
                repl.add(new NameExpr(replacementName));
            }
        }
        for (int i = 0; i < ori.size(); ++i) {
            ori.get(i).getParentNode().get().replace(ori.get(i), repl.get(i));
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
                        upperLevel.recordMockIfAbsent(argType.toString());
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
}
