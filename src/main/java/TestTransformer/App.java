package TestTransformer;

import java.io.*;
import subject.*;
import worker.*;
import storage.LineLoader;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import com.github.javaparser.ast.*;

public class App {

    public int getVal() {
        Storage.reset();
        StaticSubject.noRefun();
        NonStaticSubject nss = NonStaticSubject.create(1298, 9l, new ArrayList<>());
        nss.noReturn();
        return StaticSubject.getRefun() + nss.val() + Storage.feed + Storage.foo + nss.lift(80) + nss.lift(6);
    }

    public int fn01() {
        return new NonStaticSubject().fval();
    }

    public int fn02() {
        try {
            return StaticSubject.fval();
        } catch (Exception e) {
            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        int nProcessor = Runtime.getRuntime().availableProcessors();
        if (nProcessor > 1) {
            --nProcessor;
        }
        CountDownLatch endPoint = new CountDownLatch(nProcessor);
        BlockingQueue<String> q = new LinkedBlockingQueue();
        for (int i = 0; i < nProcessor; ++i) {
            new Thread(new UnitWorker(q, endPoint)).start();
        }
        LineLoader.loadFile("the_input_tests", line -> {
            try {
                q.put(line);
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
        try {
            for (int i = 0; i < nProcessor; ++i) {
                q.put("EOF");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        endPoint.await();
        //WoodLog.printCuts();
    }
}
