package worker;

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

    private static final Pattern RETURNABLE_MP = Pattern.compile("when\\((.+)\\.(.+)\\)\\.thenReturn\\((.+)\\)");
    private static final Pattern VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\((.+)\\)\\.(.+)");
    private static final Pattern STATIC_VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\((.+)\\.class\\)");

    private ClassWorker upperLevel;
    private Map<String, String> varTypeMap = new HashMap<>();
    private List<String> staticMockedClasses = new ArrayList<>();

    public MethodWorker(ClassWorker ul) {
        upperLevel = ul;
    }

    public void transform(MethodDeclaration methodUnit) {
        replaceMockedObject(methodUnit);
        removeMockStaticDeclaration(methodUnit);
        Node[] baseStms = getStms(methodUnit);
        boolean[] stmType = new boolean[baseStms.length];
        for (int i = 0; i < baseStms.length; i++) {
            Node n = baseStms[i];
            checkType(n);
        }
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

    private int checkType(Node node) {
        String nodeAsString = node.toString();
        Matcher returnMp = RETURNABLE_MP.matcher(nodeAsString);
        if (returnMp.find()) {
            System.out.println(returnMp.group(1)
                    + " | " + returnMp.group(2)
                    + " | " + returnMp.group(3));
        } else {
            Matcher voidMp = VOID_MP.matcher(nodeAsString);
            if (voidMp.find()) {
                System.out.println(voidMp.group(1)
                        + " | " + voidMp.group(2));
            } else {
                Matcher staticVoidMp = STATIC_VOID_MP.matcher(nodeAsString);
                if (staticVoidMp.find()) {
                    System.out.println(staticVoidMp.group(1));
                }
            }
        }
        return 0;
    }

    private void rebuildMethod(MethodDeclaration method) {
        ArrayList<String> staticMocked = new ArrayList<>();
        LinkedList<Node> useless = new LinkedList<>();
        BlockStmt methodBody = method.getBody().get();

        HashMap<String, String> varNameTypeMap = new HashMap<>();

        for (VariableDeclarator varTor: methodBody.findAll(VariableDeclarator.class)) {
            String varName = varTor.getName().asString();
            String varType = "";
            Optional<ClassOrInterfaceType> oType =  varTor.findFirst(ClassOrInterfaceType.class);
            if (oType.isPresent()) {
                varType = oType.get().getName().asString();
            } else {
                varType = varTor.findFirst(PrimitiveType.class).get().asString();
            }
            varNameTypeMap.put(varName, varType);
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
