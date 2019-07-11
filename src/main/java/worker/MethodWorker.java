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

    private static final String STATIC_VOID_RECALL_SUFFIX = "\\.([^\\(]+)\\((.*)\\)";

    private ClassWorker upperLevel;
    private Map<String, String> varTypeMap = new HashMap<>();
    private Set<String> staticMockedClasses = new HashSet<>();
    private MockMeta mockMeta = new MockMeta();

    public MethodWorker(ClassWorker ul) {
        upperLevel = ul;
    }

    public void transform(MethodDeclaration methodUnit) {
        WoodLog.reachMethod(methodUnit.getName().asString());
        replaceMockedObject(methodUnit);
        removeMockStaticDeclaration(methodUnit);
        collectVariableType(methodUnit);
        Node[] baseStms = getStms(methodUnit);
        boolean[] stmType = new boolean[baseStms.length];
        for (int i = 0; i < baseStms.length; i++) {
            Node n = baseStms[i];
            Node belowNode = null;
            if (i+1 < baseStms.length) {
                belowNode = baseStms[i+1];
            }
            checkType(n, belowNode);
        }
        System.out.println(mockMeta);
        List<Statement> replacementStms = mockRebuild();
        WoodLog.printCuts();
    }

    private void replaceMockedObject(MethodDeclaration methodUnit) {
        ArrayList<Node> ori = new ArrayList<>();
        ArrayList<Node> repl = new ArrayList<>();
        for (MethodCallExpr call: methodUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                String mockedType = call.getArguments().get(0)
                                        .findFirst(ClassOrInterfaceType.class).get()
                                        .getName().asString();;
                String replacementVarName = NameUtil.createTypeBasedName(mockedType, varTypeMap.keySet());
                varTypeMap.put(replacementVarName, mockedType);
                ori.add(call);
                repl.add(new NameExpr(replacementVarName));
            }
        }

        for (int i = 0; i < ori.size(); ++i) {
            ori.get(i).getParentNode().get().replace(ori.get(i), repl.get(i));
        }

        for (Map.Entry entry: varTypeMap.entrySet()) {
            Parameter param = buildParameterForMockedVar(entry);
            methodUnit.getParameters().add(param);
        }
    }

    private void removeMockStaticDeclaration(MethodDeclaration methodUnit) {
        LinkedList<Node> useless = new LinkedList<>();
        BlockStmt methodBody = methodUnit.getBody().get();
        for (Statement stmt: methodBody.getStatements()) {
            for (MethodCallExpr call: stmt.findAll(MethodCallExpr.class)) {
                if ("mockStatic".equals(call.getName().asString())) {
                    for (Expression ex: call.getArguments()) {
                        String className = ex.toString();
                        staticMockedClasses.add(className.substring(0, className.lastIndexOf('.')));
                    }
                    useless.push(stmt);
                }
            }
        }
        while ( ! useless.isEmpty()) {
            methodBody.remove(useless.pop());
        }
    }

    private static Node[] getStms(MethodDeclaration methodUnit) {
        ArrayList<Node> stms = new ArrayList<>();
        for(Node n: methodUnit.getBody().get().getStatements()) {
            stms.add(n);
        }
        return stms.toArray(new Node[stms.size()]);
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

            List<CallMeta> cmm = mockMeta.getBySubjectName(subject).getByMethodName(call);
            CallMeta meta = new CallMeta(param, out, outExpr, false, false);
            cmm.add(meta);
            return MOCK_STM;
        }
        Matcher voidMp = VOID_MP.matcher(nodeAsString);
        if (voidMp.find()) {
            String subject = voidMp.group(1);
            String call = voidMp.group(2);
            String param = voidMp.group(3);

            List<CallMeta> cmm = mockMeta.getBySubjectName(subject).getByMethodName(call);
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
            String pat = subject + STATIC_VOID_RECALL_SUFFIX;
            Matcher staticVoidFollowMp = Pattern.compile(pat).matcher(belowNode.toString());
            if (staticVoidFollowMp.find()) {
                String call = staticVoidFollowMp.group(1);
                String param = staticVoidFollowMp.group(2);

                List<CallMeta> cmm = mockMeta.getBySubjectName(subject).getByMethodName(call);
                CallMeta meta = new CallMeta(param, "", false, true);
                cmm.add(meta);
                return FOLLOWED_MOCK_STM;
            }
            WoodLog.attach(ERROR, subject, "<?>", CallMeta.NIL,
                    "Cannot detect static-void-mocked in followed statement: " + belowNode.toString());
            return MOCK_STM;
        }
        return NORMAL_STM;
    }

    private List<Statement> mockRebuild() {
        List<Statement> output = new ArrayList<>();
        InstanceMockWorker imw = new InstanceMockWorker();
        for (Map.Entry<String, SubjectMockMeta> smm: mockMeta.getSubjectMockMetas().entrySet()) {
            String subjectName = smm.getKey();
            if (staticMockedClasses.contains(subjectName)) {
                output.add( StaticMockWorker.transform(smm, varTypeMap) );
            } else {
                imw.addMockMeta(smm);
            }
        }
        if ( ! imw.isEmpty()) {
            output.add( imw.transform() );
        }
        System.out.println("============ mock replacement ==============");
        for (Statement stm: output) {
            System.out.println(stm);
        }
        return output;
    }

    private void collectVariableType(MethodDeclaration method) {
        for (VariableDeclarator varTor: method.findAll(VariableDeclarator.class)) {
            String varName = varTor.getName().asString();
            String varType = "";
            Optional<ClassOrInterfaceType> oType =  varTor.findFirst(ClassOrInterfaceType.class);
            if (oType.isPresent()) {
                varType = oType.get().getName().asString();
            } else {
                varType = varTor.findFirst(PrimitiveType.class).get().asString();
            }
            varTypeMap.put(varName, varType);
        }
    }

    private Parameter buildParameterForMockedVar(Map.Entry<String, String> nameType) {
        NodeList<AnnotationExpr> annotations = new NodeList<>();
        annotations.add(new MarkerAnnotationExpr("Mocked"));
        return new Parameter(
                new NodeList<>(),
                annotations,
                new ClassOrInterfaceType(nameType.getValue()),
                false,
                new NodeList<>(),
                new SimpleName(nameType.getKey())
        );
    }
}
