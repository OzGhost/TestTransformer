package worker;

import static meta.Name.INTERRUPT_SIGNAL;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import com.github.javaparser.ast.CompilationUnit;

public class UnitWorker implements Runnable {

    private BlockingQueue<String> q;
    private CountDownLatch l;

    public UnitWorker(BlockingQueue<String> iq, CountDownLatch il) {
        q = iq;
        l = il;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while ( ! Thread.currentThread().isInterrupted()) {
            WoodLog.loopLog(this, 23);
            String line = null;
            try {
                line = q.poll(10, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                System.out.println("Thread: " + Thread.currentThread().getId() + " was take down");
                e.printStackTrace();
                break;
            }
            if (INTERRUPT_SIGNAL.equals(line)) {
                break;
            }

            String of = toOutputPath(line);
            if (of.isEmpty()) continue;
            CompilationUnit cUnit = null;
            try {
                cUnit = new CompilationUnitWorker().transform(line); 
            } catch(Throwable e) {
                System.out.println("Falied at line: " + line);
                e.printStackTrace();
                continue;
            }
            if (cUnit == null) continue;
            try (FileWriter fw = new FileWriter(new File(of))) {
                fw.write(cUnit.toString().toCharArray());
                fw.flush();
            } catch(Exception e) {
                System.out.println("Failed at line: " + line);
                e.printStackTrace();
                continue;
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Thread: " + Thread.currentThread().getId() + " go down after "+((end-start)/1000)+" second!");
        l.countDown();
    }

    private String toOutputPath(String inputPath) {
        return inputPath.replace("/unittest/", "/jmocktest/");
    }
}

