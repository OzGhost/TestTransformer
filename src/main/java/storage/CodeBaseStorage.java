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

    public static String[][] findType(String[] type, String method, int parameterCount) {
        MethodDAS methodDas = packageDas.findByPackage(type[1]).findByType(type[0]);
        if (methodDas.isEmpty()) {
            String rname = type[0] + ".java";
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
                        MethodDAS methodPieces = scan(rtype, type[0]);
                        methodDas.load(methodPieces);
                        break;
                    }
                }
            }
        }
        ParameterPack paramPack = methodDas.findByMethod(method).findByParameterCount(parameterCount);
        return paramPack.getPack();
    }

    private static MethodDAS scan(CompilationUnit rtype, String type) {
        MethodDAS methodPieces = new MethodDAS();
        List<String> ims = new ArrayList<>(rtype.getImports().size());
        for (ImportDeclaration im: rtype.getImports()) {
            ims.add(im.getName().asString());
        }
        Map<String, String> imMap = NameUtil.decompileImports(ims);

        for (ClassOrInterfaceDeclaration classDecl: rtype.findAll(ClassOrInterfaceDeclaration.class)) {
            if ( ! classDecl.getName().asString().equals(type)) {
                continue;
            }
            for (MethodDeclaration methodDecl: classDecl.findAll(MethodDeclaration.class)) {
                String methodName = methodDecl.getName().asString();
                int parameterCount = methodDecl.getParameters().size();

                methodPieces
                    .findByMethod(methodName)
                    .findByParameterCount(parameterCount)
                    .setPack( loadPack(methodDecl.getParameters(), imMap) );
            }
            break;
        }
        return methodPieces;
    }

    private static String[][] loadPack(NodeList<Parameter> params, Map<String, String> imMap) {
        int len = params.size();
        String[][] out = new String[len][2];
        for (int i = 0; i < len; ++i) {
            Parameter p = params.get(i);
            Type pt = p.getType();
            String type = "";
            String im = "";
            if (pt instanceof PrimitiveType) {
                type = ((PrimitiveType)pt).asString();
            } else if (pt instanceof ClassOrInterfaceType) {
                type = ((ClassOrInterfaceType)pt).getName().asString();
                im = imMap.get(type);
                if (im == null) {
                    im = "";
                } else {
                    im += "." + type;
                }
            } else {
                WoodLog.attach(ERROR, "Found type " + pt.getClass().getCanonicalName() + " that not support yet");
                type = "String";
            }
            out[i] = new String[]{type, im};
        }
        return out;
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
            List<String> fileNames = new ArrayList<>();
            String workingDir = ClassLoader.getSystemResource(".").getPath();
            File scanOutputFile = locateScanOutputFile(workingDir);
            if (scanOutputFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(scanOutputFile))) {
                    //
                    String filename = reader.readLine();
                    while (filename != null) {
                        fileNames.add(filename);
                        filename = reader.readLine();
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                    WoodLog.attach(WARNING, "Fail to load scan output!");
                }
            }
            blackHole.accept(new VoidLoader(fileNames));
            return fileNames;
        }

        private File locateScanOutputFile(String wd) {
            char[] cs = wd.toCharArray();
            int len = cs.length;
            int i = len;
            int counter = 0;
            while (i >= 0 && counter < 5) {
                if (cs[--i] == '/') {
                    counter++;
                }
            }
            char[] o = new char[i+1+8];
            int k = i + 1;
            while (i >= 0) {
                o[i] = cs[i];
                i--;
            }
            char[] scn = "scan_out".toCharArray();
            len = scn.length;
            for (i = 0; i < len; ++i) {
                o[k+i] = scn[i];
            }
            return new File(new String(o));
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

