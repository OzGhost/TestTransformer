package worker;

import storage.LibraryImplLoader;
import reader.ReaderUtil;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import static reader.ReaderUtil.removeImportStartsWith;

public class CompilationUnitWorker {

    private Map<String, String> typeToPkgMap;
    private CompilationUnit cUnit;

    public CompilationUnit transform(String filePath) throws Exception {
        cUnit = StaticJavaParser.parse(new File(filePath));

        boolean mocked = removeImportStartsWith(cUnit, "org.mockito");
        mocked = removeImportStartsWith(cUnit, "org.powermock") || mocked;
        mocked = removeImportStartsWith(cUnit, "ch.axonivy.fintech.standard.core.mock.InvocationCounter") || mocked;
        mocked = removeImportStartsWith(cUnit, "ch.axonivy.fintech.guiframework.mock.MockUtil") || mocked;
        mocked = removeImportStartsWith(cUnit, "ch.axonivy.fintech.guiframework.mock.StaticCallVerifier") || mocked;
        for (String libImp: LibraryImplLoader.getLibImportations()) {
            mocked = removeImportStartsWith(cUnit, libImp) || mocked;
        }
        if ( ! mocked) {
            return cUnit;
        }
        removeImportStartsWith(cUnit, "org.junit.Before");

        if (removePowerMockRunner(cUnit)) {
            removeImportStartsWith(cUnit, "org.junit.runner");
        }

        cUnit.addImport(new ImportDeclaration("mockit", false, true));

        for (ClassOrInterfaceDeclaration classUnit: cUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            new ClassWorker().setCompilationUnitWorker(this).transform(classUnit);
        }

        return cUnit;
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
                        if (mv.endsWith("PowerMockRunner.class")) {
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

    public String[] findTypeByName(String type) {
        String packageOfType = findPackage(type);
        String msg = null;
        if (packageOfType == null) {
            msg = "Found no importation for type: " + type;
            // assume no import -> same package with running class
            Optional<PackageDeclaration> pkgDecl = cUnit.getPackageDeclaration();
            if ( ! pkgDecl.isPresent()) {
                WoodLog.attach(msg);
                return null; // say no more
            }
            packageOfType = pkgDecl.get().getName().asString();
            msg += " -> assume enclosed package: " + packageOfType;
        }
        if (msg != null) {
            WoodLog.attach(msg);
        }
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
