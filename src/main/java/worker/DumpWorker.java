package worker;

import storage.LineLoader;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.SimpleName;

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
        int x = 10;
        for (String tf: LineLoader.loadFile("the_input_tests")) {
            CompilationUnit cUnit = StaticJavaParser.parse(new File(tf));
            ClassOrInterfaceDeclaration clazz = cUnit.findFirst(ClassOrInterfaceDeclaration.class).get();
            String originClass = clazz.getName().asString();
            String originFile = tf.substring(0, tf.length() - 5);
            for (int i = 0; i < x; ++i) {
                String newFile = originFile + "_v" + i + ".java";
                String newClass = originClass + "_v" + i;
                clazz.setName(new SimpleName(newClass));

                try (FileWriter fw = new FileWriter(new File( newFile ))) {
                    fw.write(cUnit.toString().toCharArray());
                    fw.flush();
                }
            }
        }
    }
}

