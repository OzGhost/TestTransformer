package worker;

import static meta.Name.*;
import meta.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

public class MethodWorker {

    private static final Pattern RETURNABLE_MP = Pattern.compile("when\\((.+)\\.([^\\(]+)\\((.*)\\)\\)\\.thenReturn\\((.+)\\)");
    private static final Pattern VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\((.+)\\)\\.([^\\(]+)\\((.*)\\)");
    private static final Pattern STATIC_VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\((.+)\\.class\\)");

    private static final String STATIC_RECALL_PATTERN_SUFFIX = "\\.([^\\(]+)\\((.*)\\)";

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

        collectVariableType(methodUnit);
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
        Matcher returnMp = RETURNABLE_MP.matcher(nodeAsString);
        if (returnMp.find()) {
            String subject = returnMp.group(1);
            String call = returnMp.group(2);
            String param = returnMp.group(3);
            String out = returnMp.group(4);

            Expression outExpr = node.findFirst(MethodCallExpr.class)
                                        .get()
                                        .getArguments()
                                        .get(0);

            List<CallMeta> cmm = records.getBySubjectName(subject).getByMethodName(call);
            CallMeta meta = new CallMeta(param, out, outExpr, false, false);
            cmm.add(meta);
            return MOCK_STM;
        }
        Matcher voidMp = VOID_MP.matcher(nodeAsString);
        if (voidMp.find()) {
            String subject = voidMp.group(1);
            String call = voidMp.group(2);
            String param = voidMp.group(3);

            List<CallMeta> cmm = records.getBySubjectName(subject).getByMethodName(call);
            CallMeta meta = new CallMeta(param, "", false, true);
            cmm.add(meta);
            return MOCK_STM;
        }
        Matcher staticVoidMp = STATIC_VOID_MP.matcher(nodeAsString);
        if (staticVoidMp.find()) {
            String subject = staticVoidMp.group(1);
            if (belowNode == null) {
                WoodLog.attach(ERROR, subject, "<?>", CallMeta.NIL, "Found no co-mock void method");
                return MOCK_STM;
            }
            String pat = subject + STATIC_RECALL_PATTERN_SUFFIX;
            Matcher staticVoidFollowMp = Pattern.compile(pat).matcher(belowNode.toString());
            if (staticVoidFollowMp.find()) {
                String call = staticVoidFollowMp.group(1);
                String param = staticVoidFollowMp.group(2);

                List<CallMeta> cmm = records.getBySubjectName(subject).getByMethodName(call);
                CallMeta meta = new CallMeta(param, "", false, true);
                cmm.add(meta);
                return FOLLOWED_MOCK_STM;
            }
            WoodLog.attach(ERROR, subject, "<?>", CallMeta.NIL,
                    "Cannot detect static-void-mocked in followed statement: " + belowNode.toString());
            return MOCK_STM;
        }
        Pattern pt = Pattern.compile("verify\\(([^,]*),(.*)\\)\\.([^(]+)\\((.*)\\)");
        Matcher verifyMp = pt.matcher(nodeAsString);
        if (verifyMp.find()) {
            String subject = verifyMp.group(1);
            String fact = verifyMp.group(2);
            String methodName = verifyMp.group(3);
            String param = verifyMp.group(4);

            List<CallMeta> cmm = rechecks.getBySubjectName(subject).getByMethodName(methodName);
            CallMeta meta = new CallMeta(param, fact);
            cmm.add(meta);
            return VERIFY_STM;
        }
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
    }

}
