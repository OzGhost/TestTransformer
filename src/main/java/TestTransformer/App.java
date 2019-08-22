package TestTransformer;

import java.io.*;
import subject.*;
import worker.*;
import storage.LineLoader;
import java.util.List;
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

    public static void main(String[] args) throws Exception {
        List<String> lines = LineLoader.loadFile("the_input_tests");
        int len = lines.size();
        for (int i = 0; i < len; ++i) {
            String f = lines.get(i);
            String of = click(f);
            if (of.isEmpty()) continue;
            System.out.println("Processing: " + f);
            CompilationUnit cUnit = new CompilationUnitWorker().transform(f); 
            //if (true) break;
            try (FileWriter fw = new FileWriter(new File(of))) {
                fw.write(cUnit.toString().toCharArray());
                fw.flush();
            } catch(Exception e) {
                e.printStackTrace();
            }
            //break;
        }
        //CompilationUnit cUnit = null;//new CompilationUnitWorker().transform("./src/test/java/TestTransformer/MockTest.java");
        WoodLog.printCuts();
    }

    private static String click(String input) {
        int ui = input.indexOf("unittest");
        if (ui < 0) return "";
        char[] ci = input.toCharArray();
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
