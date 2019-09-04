package worker;

import java.io.*;
import java.util.concurrent.BlockingQueue;
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
        while (true) {
            String line = null;
            try {
                line = q.take();
            } catch(Exception e) {
                l.countDown();
                Thread.currentThread().interrupt();
            }
            if ("EOF".equals(line)) {
                break;
            }
            //System.out.println("["+Thread.currentThread().getId()+"] Processing: " + line);

            String of = toOutputPath(line);
            if (of.isEmpty()) continue;
            CompilationUnit cUnit = null;
            try {
                cUnit = new CompilationUnitWorker().transform(line); 
            } catch(Exception e) {
                e.printStackTrace();
            }
            if (cUnit == null) continue;
            try (FileWriter fw = new FileWriter(new File(of))) {
                fw.write(cUnit.toString().toCharArray());
                fw.flush();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        l.countDown();
    }

    private String toOutputPath(String inputPath) {
        int ui = inputPath.indexOf("unittest");
        if (ui < 0) return "";
        char[] ci = inputPath.toCharArray();
        char[] oi = new char[ci.length + 1];
        int k = 0;
        for (int i = 0; i < ui; ++i) {
            oi[k++] = ci[i];
        }
        char[] ji = "jmocktest".toCharArray();
        for (int i = 0; i < ji.length; ++i) {
            oi[k++] = ji[i];
        }
        ui += 8;
        while (ui < ci.length) {
            oi[k++] = ci[ui++];
        }
        return new String(oi);
    }
}

