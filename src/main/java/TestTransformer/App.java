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

public class App {

    public static void main(String[] args) throws Exception {
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
        for (int i = 0; i < nProcessor; ++i) {
            new Thread(new UnitWorker(q, endPoint)).start();
        }
        LineLoader.loadFile("the_input_tests", line -> {
            try {
                q.put(line);
            } catch(Exception e) {
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
