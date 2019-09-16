package TestTransformer;

import static meta.Name.INTERRUPT_SIGNAL;
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

    public App() {
        NeutralSubject.doNoop();
        NonStaticSubject.doNoop();
        StaticSubject.doNoop();
    }

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

    public int fn03() {
        NonStaticSubject s = new NonStaticSubject();
        return s.fval() + s.sval();
    }
    public int fn03_1() {
        NonStaticSubject s = StaticSubject.getNext();
        return s.fval() + s.sval();
    }
    public int fn03_2() {
        NonStaticSubject s = NonStaticSubject.create(1298, 9l, new ArrayList<>());
        return s.fval() + s.sval();
    }
    public int fn04() {
        //NonStaticSubject s = new NonStaticSubject();
        NonStaticSubject s = StaticSubject.getNext();
        //System.out.println("sval: " + s.sval());
        s.reset();
        return s.fval() + s.sval();
    }

    public int fn05() {
        return StaticSubject.fn05();
    }

    public void fn06() {
        StaticSubject.fn06();
    }

    public static void main(String[] args) throws Exception {
        boolean real = false;
        if ( ! real) {
            CompilationUnit cUnit = new CompilationUnitWorker().transform("./src/test/java/TestTransformer/MockTest.java");
            System.out.println(cUnit);
            //WoodLog.printCuts();
            return;
        }
        int nProcessor = Runtime.getRuntime().availableProcessors();
        if (nProcessor > 1) {
            --nProcessor;
        }
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
        //WoodLog.printCuts();
    }
}
