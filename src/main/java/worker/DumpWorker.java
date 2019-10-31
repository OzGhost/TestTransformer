package worker;

import reader.ReaderUtil;
import storage.LineLoader;

import java.util.List;
import java.util.LinkedList;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;

public class DumpWorker implements Runnable {

    private CountDownLatch l;

    public DumpWorker(CountDownLatch latch) {
        l = latch;
    }

    @Override
    public void run() {
        Thread me = Thread.currentThread();
        while( ! me.isInterrupted()) {
        //while( true ) {
            System.out.println("Running: " + me.getId() + " interrupted: " + me.isInterrupted());
            if (false) break;
        }
        System.out.println("T " + me.getId() + " going down!");
        l.countDown();
    }

    public static void dump() throws Exception {
        int x = 50;
        for (String tf: LineLoader.loadFile("the_input_tests")) {
            CompilationUnit cUnit = StaticJavaParser.parse(new File(tf));
            ClassOrInterfaceDeclaration clazz = cUnit.findFirst(ClassOrInterfaceDeclaration.class).get();
            String originClass = clazz.getName().asString();
            originClass = originClass.substring(0, originClass.length() - 4);
            String originFile = tf.substring(0, tf.length() - 9);
            for (int i = 0; i < x; ++i) {
                String newFile = originFile + "_v" + i + "_Test.java";
                String newClass = originClass + "_v" + i + "_Test";
                clazz.setName(new SimpleName(newClass));

                try (FileWriter fw = new FileWriter(new File( newFile ))) {
                    fw.write(cUnit.toString().toCharArray());
                    fw.flush();
                }
            }
        }
    }

    public static void setUpMock(CompilationUnit cUnit) {
        List<MethodCallExpr> calls = new LinkedList<>();
        for (MethodCallExpr c: cUnit.findAll(MethodCallExpr.class)) {
            if ("mock".equals(c.getName().asString())) {
                calls.add(c);
            }
        }
        for (MethodCallExpr c: calls) {
            setUpMethodCall(c);
        }
        List<FieldDeclaration> fields = new LinkedList<>();
        for (FieldDeclaration f: cUnit.findAll(FieldDeclaration.class)) {
            if (ReaderUtil.hasAnnotation(f, "Mock")) {
                fields.add(f);
            }
        }
        for (FieldDeclaration f: fields) {
            setUpFieldMock(f);
        }
    }

    private static void setUpMethodCall(MethodCallExpr c) {
        if (c.getArguments().size() > 1) return;
        c.getArguments().add(new NameExpr("org.mockito.Mockito.withSettings().stubOnly()"));
    }

    private static void setUpFieldMock(FieldDeclaration f) {
        AnnotationExpr useless = null;
        for (AnnotationExpr a: f.getAnnotations()) {
            if ("Mock".equals(a.getName().asString())) {
                useless = a;
                break;
            }
        }
        for (VariableDeclarator v: f.getVariables()) {
            String name = v.getType().asString().replaceAll("<.+>", "");
            v.setInitializer(new NameExpr("org.mockito.Mockito.mock("+name+".class , org.mockito.Mockito.withSettings().stubOnly())"));
        }
        useless.remove();
    }
}

