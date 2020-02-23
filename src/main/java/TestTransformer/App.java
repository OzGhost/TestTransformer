package TestTransformer;

import static meta.Name.INTERRUPT_SIGNAL;
import java.io.*;
import worker.*;
import storage.LineLoader;
import storage.LibraryImplLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import com.github.javaparser.ast.*;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import worker.CompilationUnitWorker;

public class App {

    public static void main(String[] args) throws Exception {
        workLoad();
        //testLoad();
    }

    private static void testLoad() throws Exception {
        int counter = 0;
        for (File f: new File("src/test/java/cases").listFiles()) {
            if (f.getName().endsWith("TestConverted.java")) {
                f.delete();
                counter++;
            }
        }
        System.out.println("testLoad >> " + counter + " test file was wash away.");
        counter = 0;
        for (File f: new File("src/test/java/cases").listFiles()) {
            String fpath = f.getAbsolutePath();
            if (fpath.endsWith("Test.java")) {
                int preLastDotIndex = fpath.lastIndexOf((int)'/');
                String prefix = fpath.substring(0, preLastDotIndex+1);
                String className = fpath.substring(preLastDotIndex+1, fpath.length()-5);

                CompilationUnit cUnit = new CompilationUnitWorker().transform(fpath); 
                Optional<ClassOrInterfaceDeclaration> classDecl = cUnit.getClassByName(className);
                if ( ! classDecl.isPresent()) {
                    continue;
                }
                String newName = className + "Converted";
                classDecl.get().setName(newName);
                try (FileWriter fw = new FileWriter(new File(prefix + newName + ".java"))) {
                    fw.write(cUnit.toString().toCharArray());
                    fw.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
                counter++;
            }
        }
        System.out.println("testLoad >> " + counter + " test file was generated");
    }

    private static void workLoad() throws Exception {
        LibraryImplLoader.load();
        boolean real = true;
        if ( ! real) {
            String targetFile = "";
            CompilationUnit cUnit = new CompilationUnitWorker().transform(targetFile);
            String of = targetFile.substring(0, targetFile.length() - 5) + "Sub.java";
            try (FileWriter fw = new FileWriter(new File(of))) {
                fw.write(cUnit.toString().toCharArray());
                fw.flush();
            } catch(Exception e) {
                e.printStackTrace();
            }
            return;
        }
        int nProcessor = Runtime.getRuntime().availableProcessors();
        //nProcessor = 1;
        CountDownLatch endPoint = new CountDownLatch(nProcessor);
        BlockingQueue<String> q = new LinkedBlockingQueue<>();
        Thread[] deciples = new Thread[nProcessor];
        for (int i = 0; i < nProcessor; ++i) {
            deciples[i] = new Thread(new UnitWorker(q, endPoint));
            deciples[i].start();
        }
        LineLoader.loadFile("the_input_tests", line -> {
            try {
                q.put(line);
            } catch(Exception e) {
                for (Thread t: deciples) {
                    t.interrupt();
                }
                throw new RuntimeException(e);
            }
        });
        try {
            for (int i = 0; i < nProcessor; ++i) {
                q.put(INTERRUPT_SIGNAL);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        endPoint.await();
    }
}

