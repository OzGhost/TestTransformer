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
    private Map<String, Type> varTypeMap = new HashMap<>();
    private Set<String> mockedTypes = new HashSet<>();
    private MockingMeta records = new MockingMeta();
    private MockingMeta rechecks = new MockingMeta();


    public MethodWorker(ClassWorker ul) {
        upperLevel = ul;
    }

    public void transform(MethodDeclaration methodUnit) {
        WoodLog.reachMethod(methodUnit.getName().asString());

        //collectVariableType(methodUnit);
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

    private void collectVariableType(MethodDeclaration method) {
        for (VariableDeclarator varTor: method.findAll(VariableDeclarator.class)) {
            String varName = varTor.getName().asString();
            Type varType = varTor.getType();
            varTypeMap.put(varName, varType);
        }
    }

    private void replaceInstanceMockDeclaration(MethodDeclaration methodUnit) {
        List<Node> ori = new ArrayList<>();
        List<Node> repl = new ArrayList<>();
        List<Type> instanceMockTypes = new ArrayList<>();
        List<String> instanceMockNames = new ArrayList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                Type mockingType = ((ClassExpr)call.getArguments().get(0)).getType();
                String replacementName = NameUtil.createTypeBasedName(mockingType.toString(), varTypeMap.keySet());

                instanceMockTypes.add(mockingType);
                instanceMockNames.add(replacementName);

                ori.add(call);
                repl.add(new NameExpr(replacementName));
            }
        }
        for (int i = 0; i < ori.size(); ++i) {
            ori.get(i).getParentNode().get().replace(ori.get(i), repl.get(i));
        }
        declareTypesAsMocked(instanceMockTypes, instanceMockNames, methodUnit.getParameters());
    }

    private void declareTypesAsMocked(List<Type> argTypes, List<String> argNames, NodeList<Parameter> currentParameters) {
        for (int i = 0; i < argTypes.size(); ++i) {
            declareTypeAsMocked(argTypes.get(i), argNames.get(i), currentParameters, false);
        }
    }

    private void declareTypeAsMocked(Type argType, String argName, NodeList<Parameter> currentParameters, boolean requiredUnique) {
        String typeAsString = argType.toString();
        if ( requiredUnique && mockedTypes.contains(typeAsString) ) {
            return;
        }
        Parameter param =  new Parameter(
                new NodeList<>(),
                new NodeList<>(new MarkerAnnotationExpr("Mocked")),
                argType,
                false,
                new NodeList<>(),
                new SimpleName(argName)
        );
        currentParameters.add(param);
        // track down mocked types
        mockedTypes.add(typeAsString);
        // track down variable and it type
        varTypeMap.put(argName, argType);
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
                        staticMocks.add(argType);
                    }
                    useless.push(stmt);
                }
            }
        }
        while ( ! useless.isEmpty()) {
            methodBody.remove(useless.pop());
        }
        NodeList<Parameter> currentParameters = methodUnit.getParameters();
        declareTypesAsMocked(staticMocks, currentParameters);
    }

    private void declareTypesAsMocked(List<Type> mockingTypes, NodeList<Parameter> currentParameters) {
        for (Type mockingType: mockingTypes) {
            String argName = NameUtil.createTypeBasedName(mockingType.toString(), varTypeMap.keySet());
            declareTypeAsMocked(mockingType, argName, currentParameters, true);
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
        /*
        pt = Pattern.compile("verifyStatic\\(([^,]*)\\.class,(.*)\\)");
        Matcher verifyStaticMp = pt.matcher(nodeAsString);
        if (verifyStaticMp.find()) {
            String subject = verifyStaticMp.group(1);
            String fact = verifyStaticMp.group(2);
            String staticRecallPattern = subject + STATIC_RECALL_PATTERN_SUFFIX;
            Matcher recallMatcher = Pattern.compile(staticRecallPattern).matcher(belowNode.toString());
            if (recallMatcher.find()) {
                String methodName = recallMatcher.group(1);
                String param = recallMatcher.group(2);

                List<CallMeta> cmm = rechecks.getBySubjectName(subject).getByMethodName(methodName);
                CallMeta meta = new CallMeta(param, fact);
                cmm.add(meta);
                return FOLLOWED_VERIFY_STM;
            }
            WoodLog.attach(ERROR, subject, "<?>", CallMeta.NIL,
                    "Cannot detect static-verify call in follow statement: " + belowNode.toString());
            return VERIFY_STM;
        }
        return NORMAL_STM;
        */
    }

}
