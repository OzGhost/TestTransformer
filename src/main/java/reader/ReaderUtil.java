package reader;

import static meta.Name.*;
import worker.WoodLog;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;

public class ReaderUtil {

    private static Pattern IC_EQ_P = Pattern.compile("assertEquals\\((\\d+)\\s*,\\s*[a-zA-Z0-9_$]+\\.times\\(\\)\\)");
    private static Pattern IC_THAT_P = Pattern.compile("assertThat\\([a-zA-Z0-9_$]+\\.times\\(\\)\\s*,\\s*[a-zA-Z0-9_$]*?\\.?(?:is|equalTo)\\((\\d+)\\)\\)");

    private ReaderUtil() {
        throw new UnsupportedOperationException();
    }

    public static Expression getThenReturnExpr(Node inputNode) {
        return getFirstArgumentExpr(inputNode, "thenReturn");
    }

    public static Expression getDoReturnExpr(Node inputNode) {
        return getFirstArgumentExpr(inputNode, "doReturn");
    }

    private static Expression getFirstArgumentExpr(Node inputNode, String methodName) {
        for (MethodCallExpr m: inputNode.findAll(MethodCallExpr.class)) {
            if (methodName.equals(m.getName().asString())) {
                NodeList<Expression> args = m.getArguments();
                if (args.size() == 1) {
                    return args.get(0);
                }
            }
        }
        WoodLog.attach(WARNING, "Found no '"+methodName+"' phase in: "+inputNode.toString());
        return null;
    }

    public static Expression getThenThrowExpr(Node inputNode) {
        return getFirstArgumentExpr(inputNode, "thenThrow");
    }

    public static int getICExpectedTimes(String stm) {
        Matcher icm = IC_EQ_P.matcher(stm);
        if ( icm.find() ) {
            return Integer.parseInt(icm.group(1));
        }
        icm = IC_THAT_P.matcher(stm);
        if ( icm.find() ) {
            return Integer.parseInt(icm.group(1));
        }
        return -1;
    }

    public static <T extends Node> T findClosestParent(Node currentNode, Class<T> parentType) {
        Node target = null;
        Optional<Node> on = currentNode.getParentNode();
        while (on.isPresent()) {
            target = on.get();
            //System.out.println(target.getClass());
            if (target.getClass() == parentType) break;
            on = target.getParentNode();
        }
        if (target.getClass() == parentType) return parentType.cast(target);
        return null;
    }

    public static boolean removeImportStartsWith(CompilationUnit cUnit, String importPrefix) {
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

    public static void eliminateImportation(CompilationUnit cUnit) {
        removeImportStartsWith(cUnit, "org.mockito");
        removeImportStartsWith(cUnit, "org.powermock");

        Map<String, ImportRemovalPack> typeLeadMap = new HashMap<>();

        List<ImportDeclaration> useless = new LinkedList<>();
        for (ImportDeclaration im: cUnit.getImports()) {
            useless.add(im);
            String pkg = im.getName().asString();
            String type = pkg.substring(pkg.lastIndexOf('.')+1);
            typeLeadMap.put(type, new ImportRemovalPack(pkg));
        }
        for (ImportDeclaration u: useless) u.remove();
        for (ClassOrInterfaceType c: cUnit.findAll(ClassOrInterfaceType.class)) {
            if (c.getScope().isPresent()) continue;
            String typeAsString = c.getName().asString();
            ImportRemovalPack irp = typeLeadMap.get(typeAsString);
            if (irp != null) {
                irp.fellow.add(c);
            }
        }
        for (ImportRemovalPack irp: typeLeadMap.values()) {
            SimpleName repName = new SimpleName(irp.pkg);
            for (Node f: irp.fellow) {
                ClassOrInterfaceType t = (ClassOrInterfaceType) f;
                t.setName(repName);
            }
            irp.fellow = new LinkedList<>();
        }
        for (MethodCallExpr call: cUnit.findAll(MethodCallExpr.class)) {
            Optional<Expression> scopeOp = call.getScope();
            if ( ! scopeOp.isPresent()) continue;
            Expression scope = scopeOp.get();
            if ( ! (scope instanceof NameExpr)) continue;
            NameExpr caller = (NameExpr)scope;
            String callerAsString = caller.getName().asString();
            ImportRemovalPack irp = typeLeadMap.get(callerAsString);
            if (irp != null) {
                irp.fellow.add(caller);
            }
        }
        for (FieldAccessExpr fa: cUnit.findAll(FieldAccessExpr.class)) {
            Expression scope = fa.getScope();
            if ( ! (scope instanceof NameExpr)) continue;
            NameExpr caller = (NameExpr) scope;
            String callerAsString = caller.getName().asString();
            ImportRemovalPack irp = typeLeadMap.get(callerAsString);
            if (irp != null) {
                irp.fellow.add(caller);
            }
        }
        for (ImportRemovalPack irp: typeLeadMap.values()) {
            SimpleName rep = new SimpleName(irp.pkg);
            for (Node f: irp.fellow) {
                ((NameExpr)f).setName(rep);
            }
            irp.fellow = new LinkedList<>();
        }
    }

    private static class ImportRemovalPack {
        String pkg;
        List<Node> fellow = new LinkedList<>();
        ImportRemovalPack(String p) {
            pkg = p;
        }
    }

    public static boolean hasAnnotation(BodyDeclaration<?> subject, String annotationName) {
        for (AnnotationExpr ann: subject.getAnnotations()) {
            if (ann.getName().asString().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAType(String type) {
        char firstChar = type.charAt(0);
        int lastDotIndex = type.lastIndexOf('.');
        if (lastDotIndex > 0) {
            firstChar = type.charAt(lastDotIndex+1);
        }
        return 'A' <= firstChar && firstChar <= 'Z';
    }

    public static String[] depart(String type) {
        int lastDotIndex = type.lastIndexOf('.');
        if (lastDotIndex > 0) {
            char firstChar = type.charAt(lastDotIndex+1);
            if ('A' <= firstChar && firstChar <= 'Z'){
                String typename = type.substring(lastDotIndex+1);
                String pkg = type.substring(0, lastDotIndex);
                return new String[]{ typename, pkg };
            }
        }
        return null;
    }
}

