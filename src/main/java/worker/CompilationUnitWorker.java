package worker;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

public class CompilationUnitWorker {

    private static final int CASE_DELTA = (int)'a' - (int)'A';

    public CompilationUnit transform(String filePath) throws Exception {
        CompilationUnit cUnit = StaticJavaParser.parse(new File("./src/test/java/TestTransformer/MockTest.java"));
        //CompilationUnit cUnit = StaticJavaParser.parse(new File("./src/test/java/TestTransformer/AlterTest.java"));
        //Printer.print(cUnit);

        boolean mocked = removeImportStartsWith(cUnit, "org.mockito");
        mocked = removeImportStartsWith(cUnit, "org.powermock") || mocked;
        if ( ! mocked) {
            return cUnit;
        }

        if (removePowerMockRunner(cUnit)) {
            removeImportStartsWith(cUnit, "org.junit.runner");
        }

        cUnit.addImport(new ImportDeclaration("mockit", false, true));

        for (ClassOrInterfaceDeclaration classUnit: cUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            for (MethodDeclaration methodUnit: classUnit.findAll(MethodDeclaration.class)) {
                rebuildMethod(methodUnit);
            }
        }

        //System.out.println(cUnit);
        //Printer.print(cUnit);
    }

    private boolean removeImportStartsWith(CompilationUnit cUnit, String importPrefix) {
        NodeList<ImportDeclaration> imports = cUnit.getImports();
        ArrayList<ImportDeclaration> useless = new ArrayList<>(imports.size());
        for (ImportDeclaration imp: imports) {
            String name = imp.getName().asString();
            if (name.startsWith(importPrefix)) {
                useless.add(imp);
            }
        }
        for (ImportDeclaration imp: useless) {
            imports.remove(imp);
        }
        return !useless.isEmpty();
    }

    private boolean removePowerMockRunner(CompilationUnit cUnit) {
        boolean output = false;
        for (ClassOrInterfaceDeclaration classNode: cUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            ArrayList<Node> useless = new ArrayList<>();
            for (Node cn: classNode.getChildNodes()) {
                if (cn instanceof SingleMemberAnnotationExpr) {
                    SingleMemberAnnotationExpr acn = (SingleMemberAnnotationExpr) cn;
                    String identifier = acn.getName().getIdentifier();
                    if ("RunWith".equals(identifier)) {
                        String mv = acn.getMemberValue().toString();
                        if ("PowerMockRunner.class".equals(mv)) {
                            output = true;
                            useless.add(cn);
                        }
                    } else if ("PrepareForTest".equals(identifier)){
                        output = true;
                        useless.add(cn);
                    }
                }
            }
            for (Node un: useless) {
                classNode.remove(un);
            }
        }
        return output;
    }

    private void rebuildMethod(MethodDeclaration method) {
        ArrayList<String> staticMocked = new ArrayList<>();
        LinkedList<Node> useless = new LinkedList<>();
        BlockStmt methodBody = method.getBody().get();

        ArrayList<Node> ori = new ArrayList<>();
        ArrayList<Node> repl = new ArrayList<>();
        HashMap<String, String> varNameTypeMap = new HashMap<>();

        for (MethodCallExpr call: methodBody.findAll(MethodCallExpr.class)) {
            if ("mock".equals(call.getName().asString())) {
                String mockedType = call.getArguments().get(0)
                    .findFirst(ClassOrInterfaceType.class).get()
                    .getName().asString();;
                String replacementVarName = buildVariableName(mockedType, varNameTypeMap.keySet());
                varNameTypeMap.put(replacementVarName, mockedType);
                ori.add(call);
                repl.add(new NameExpr(replacementVarName));
            }
        }

        for (int i = 0; i < ori.size(); ++i) {
            ori.get(i).getParentNode().get().replace(ori.get(i), repl.get(i));
        }

        for (Map.Entry entry: varNameTypeMap.entrySet()) {
            Parameter param = buildParameterForMockedVar(entry);
            method.getParameters().add(param);
        }

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

        /*
        for (Map.Entry entry: varNameTypeMap.entrySet()) {
            System.out.println("found -->> var: " + entry.getKey() + " with type: " + entry.getValue());
        }
        */

        for (Statement stmt: methodBody.getStatements()) {
            for (MethodCallExpr call: stmt.findAll(MethodCallExpr.class)) {
                if ("mockStatic".equals(call.getName().asString())) {
                    for (Expression ex: call.getArguments()) {
                        String className = ex.toString();
                        staticMocked.add(className.substring(0, className.lastIndexOf('.')));
                    }
                    useless.push(stmt);
                }
            }
        }
        //staticMocked.forEach(e -> System.out.println("-->>> " + e));
        while ( ! useless.isEmpty()) {
            methodBody.remove(useless.pop());
        }

        for (Statement stm: methodBody.getStatements()) {
            String stmas = stm.toString();
            if (stmas.contains("when")) {
                System.out.println(stmas);
            }
        }
    }

    private String buildVariableName(String varType, Set<String> usedVarName) {
        char firstCharLowerCase = (char)( (int) varType.charAt(0) + CASE_DELTA );
        String base = new StringBuilder(varType.length())
                            .append(firstCharLowerCase)
                            .append(varType.substring(1, varType.length()))
                            .toString();
        String output = base;
        int version = 1;
        while (usedVarName.contains(output)) {
            version++;
            output = base + "_v" + version;
        }
        return output;
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
