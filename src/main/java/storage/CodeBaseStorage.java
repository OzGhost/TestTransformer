package storage;

import java.util.Optional;
import worker.WoodLog;
import worker.NameUtil;
import static meta.Name.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;

public class CodeBaseStorage {

    private static final PackageDAS packageDas = new PackageDAS();

    private static Loader loader = new LazyLoader(CodeBaseStorage::setLoader);

    private static void setLoader(Loader l) {
        loader = l;
    }

    public static CodeBaseStorage i() {
        return new CodeBaseStorage();
    }

    public MethodDesc findMethodDesc(String[] type, String method, int parameterCount) {
        MethodDAS methodDas = findClassDesc(type);
        return methodDas.findByMethod(method).findByParameterCount(parameterCount);
    }

    private MethodDAS findClassDesc(String[] type) {
        //System.out.println("Search around for: " + type[1] + "." + type[0]);
        MethodDAS methodDas = packageDas.findByPackage(type[1]).findByType(type[0]);
        boolean found = false;
        if ( ! methodDas.isEmpty()) {
            return methodDas;
        }
        String rname = '/' + type[0] + ".java";
        for (String fileName: loader.load()) {
            if (fileName.endsWith(rname)) {
                CompilationUnit rtype = null;
                try {
                    rtype = StaticJavaParser.parse(new File(fileName));
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                    WoodLog.attach(WARNING, "Failed to load file " + fileName);
                    continue;
                }
                Optional<PackageDeclaration> pkgDecl = rtype.getPackageDeclaration();
                if ( ! pkgDecl.isPresent()) {
                    continue;
                }
                String pkg = pkgDecl.get().getName().asString();
                if (type[1].equals(pkg)) {
                    found = true;
                    Map<String, String> imMap = loadImMap(rtype);
                    ClassOrInterfaceDeclaration cld = findTarget(rtype, type[0]);
                    MethodDAS methodPieces = scan(cld, imMap);
                    methodDas.load(methodPieces);
                    loadSuperClassDesc(cld, methodDas, imMap);
                    break;
                } else {
                    WoodLog.attach("Found a unmatched file "+fileName+". Expect: [" + type[1] + "] but was [" + pkg + "]");
                }
            }
        }
        if ( ! found) {
            WoodLog.attach("Found no code base file for type: " + type[1] + "." + type[0]);
        }
        return methodDas;
    }

    private Map<String, String> loadImMap(CompilationUnit rtype) {
        List<String> ims = new ArrayList<>(rtype.getImports().size());
        for (ImportDeclaration im: rtype.getImports()) {
            ims.add(im.getName().asString());
        }
        return NameUtil.decompileImports(ims);
    }

    private ClassOrInterfaceDeclaration findTarget(CompilationUnit rtype, String cl) {
        return rtype.getClassByName(cl).orElseThrow(() -> new RuntimeException("Cannot find class with name: " + cl));
    }

    private MethodDAS scan(ClassOrInterfaceDeclaration classDecl, Map<String, String> imMap) {
        MethodDAS methodPieces = new MethodDAS();
        for (MethodDeclaration methodDecl: classDecl.findAll(MethodDeclaration.class)) {
            String methodName = methodDecl.getName().asString();
            int parameterCount = methodDecl.getParameters().size();

            MethodDesc md = methodPieces
                .findByMethod(methodName)
                .findByParameterCount(parameterCount);

            loadDes(md, methodDecl, imMap);
        }
        return methodPieces;
    }

    private void loadDes(MethodDesc md, MethodDeclaration methodDecl, Map<String, String> imMap) {
        NodeList<Parameter> params = methodDecl.getParameters();
        int len = params.size();
        String[][] param = new String[len][2];
        for (int i = 0; i < len; ++i) {
            Parameter p = params.get(i);
            Type pt = p.getType();
            param[i] = getType(pt, imMap);
        }
        md.setParamTypes(param);
        md.setArguments(params);
        md.setReturnType(methodDecl.getType());
        md.setExceptions(methodDecl.getThrownExceptions());
    }

    private String[] getType(Type pt, Map<String, String> imMap) {
        String type = "";
        String pkg = "";
        if (pt instanceof PrimitiveType) {
            type = ((PrimitiveType)pt).asString();
        } else if (pt instanceof ClassOrInterfaceType) {
            type = ((ClassOrInterfaceType)pt).getName().asString();
            pkg = imMap.get(type);
            if (pkg == null) {
                pkg = "";
            }
        } else {
            WoodLog.attach(ERROR, "Found type " + pt.getClass().getCanonicalName() + " that not support yet");
            type = "String";
        }
        return new String[]{ type, pkg };
    }

    private void loadSuperClassDesc(ClassOrInterfaceDeclaration classDecl, MethodDAS methodDas, Map<String, String> imMap) {
        for (ClassOrInterfaceType t: classDecl.getExtendedTypes()) {
            String[] tt = getType(t, imMap);
            //System.out.println("Found super: " + tt[1] + '.' + tt[0]);
            MethodDAS superDesc = findClassDesc(tt);
            methodDas.merge(superDesc);
        }
    }

    private static interface Loader {
        public List<String> load();
    }

    private static class LazyLoader implements Loader {

        private Consumer<Loader> blackHole;

        public LazyLoader(Consumer<Loader> bh) {
            blackHole = bh;
        }

        @Override
        public List<String> load() {
            List<String> fileNames = LineLoader.loadFile("scan_out");
            blackHole.accept(new VoidLoader(fileNames));
            return fileNames;
        }
    }

    private static class VoidLoader implements Loader {

        private List<String> fileNames;

        public VoidLoader(List<String> fns) {
            fileNames = fns;
        }

        @Override
        public List<String> load() {
            return fileNames;
        }
    }
}

