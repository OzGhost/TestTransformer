package storage;

import worker.Normalizer;
import static meta.Name.WARNING;
import worker.WoodLog;
import worker.SignatureService;
import reader.ReaderUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import static reader.ReaderUtil.removeImportStartsWith;

public class LibraryImplLoader {

    private static final Map<String, Lib> libMap = new HashMap<>();
    private static final Set<String> supportedLib = new HashSet<>();
    private static final Set<String> supportedMethod = new HashSet<>();
    private static final Set<String> libImportations = new HashSet<>();

    public static MethodDeclaration find(String className, String methodName) {
        Lib lib = libMap.get(className);
        if (lib == null) return null;
        return lib.methodMap.get(methodName);
    }

    public static boolean isSupportedMethod(String methodName) {
        return supportedMethod.contains(methodName);
    }

    public static boolean isSupportedLib(String className) {
        return supportedLib.contains(className);
    }

    public static Set<String> getLibImportations() {
        return libImportations;
    }

    public static List<String> getIms(String className) {
        Lib lib = libMap.get(className);
        if (lib == null) return new LinkedList<>();
        return lib.ims;
    }
    
    public static void load() {
        for (String lib: LineLoader.loadFile("uim")) {
            String lt = lib.trim();
            if ( ! lt.isEmpty()) {
                loadFile(lt);
            }
        }
    }

    private static void loadFile(String fileName) {
        CompilationUnit cUnit = null;
        try {
            cUnit = StaticJavaParser.parse(new File(fileName));
        } catch(FileNotFoundException ignored) {
        }
        if (cUnit == null) {
            WoodLog.attach(WARNING, "Cannot read the lib: " + fileName);
            return;
        }
        ReaderUtil.eliminateImportation(cUnit);
        
        String pkg = cUnit.getPackageDeclaration().get().getName().asString();
        List<String> ims = new LinkedList<>();
        //for (ImportDeclaration im: cUnit.getImports()) {
            //ims.add(im.getName().asString());
        //}
        for (ClassOrInterfaceDeclaration classUnit: cUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            loadClass(classUnit, pkg, ims);
        }
    }

    private static void loadClass(ClassOrInterfaceDeclaration rawClass, String pkg, List<String> ims) {
        ClassOrInterfaceDeclaration classUnit = new Normalizer().normalize(rawClass);
        String className = classUnit.getName().asString();
        if (libMap.containsKey(className)) {
            WoodLog.attach(WARNING, "Duplicate lib class: " + className);
            return;
        }
        Map<String, MethodDeclaration> methodMap = new HashMap<>();
        for (MethodDeclaration mUnit: classUnit.findAll(MethodDeclaration.class)) {
            mUnit.getModifiers().add( new Modifier(Modifier.Keyword.PRIVATE) );
            String methodSig = SignatureService.extractSignature(mUnit);
            if (methodMap.containsKey(methodSig)) {
                //System.out.println("Duplicate lib funciton: " + className+":"+methodSig);
                //WoodLog.attach(WARNING, "Duplicate lib funciton: " + className+":"+methodSig);
                continue;
            }
            methodMap.put(methodSig, mUnit);
            supportedMethod.add(methodSig);
        }

        Lib lib = new Lib();
        lib.methodMap = methodMap;
        lib.ims = ims;

        libMap.put(className, lib);
        supportedLib.add(className);
        libImportations.add(pkg+"."+className);
    }

    private static class Lib {
        Map<String, MethodDeclaration> methodMap;
        List<String> ims;
    }

}

