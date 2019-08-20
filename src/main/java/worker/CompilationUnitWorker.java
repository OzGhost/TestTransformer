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

    private Map<String, String> typeToPkgMap;
    private CompilationUnit cUnit;

    public CompilationUnit transform(String filePath) throws Exception {
        cUnit = StaticJavaParser.parse(new File(filePath));
        //CompilationUnit cUnit = StaticJavaParser.parse(new File("./src/test/java/TestTransformer/AlterTest.java"));
        //Printer.print(cUnit);
        //if (true) return cUnit;

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
            new ClassWorker().setCompilationUnitWorker(this).transform(classUnit);
        }

        //System.out.println(cUnit);
        //Printer.print(cUnit);
        return cUnit;
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

    public void addImportationIfAbsent(String im) {
        if (cUnit != null) {
            NodeList<ImportDeclaration> ims = cUnit.getImports();
            boolean imported = false;
            for (ImportDeclaration i: ims) {
                if (i.getName().asString().equals(im)) {
                    imported = true;
                    break;
                }
            }
            if ( ! imported) {
                ims.add(new ImportDeclaration(im, false, false));
            }
        }
    }

    public String[] findType(String type) {
        String packageOfType = findPackage(type);
        return new String[]{type, packageOfType};
    }

    private String findPackage(String type) {
        if (typeToPkgMap == null) {
            typeToPkgMap = new HashMap<>();
            if (cUnit != null) {
                List<String> ims = new ArrayList<>(cUnit.getImports().size());
                for (ImportDeclaration im: cUnit.getImports()) {
                    if ( ! im.isAsterisk()) {
                        ims.add(im.getName().asString());
                    }
                }
                typeToPkgMap = NameUtil.decompileImports(ims);
            }
        }
        return typeToPkgMap.get(type);
    }
}
