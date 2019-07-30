package TestTransformer;

import subject.*;
import worker.*;
import java.util.ArrayList;

public class App {

    public int getVal() {
        Storage.reset();
        StaticSubject.noRefun();
        NonStaticSubject nss = NonStaticSubject.create(1298, 9l, new ArrayList<>());
        nss.noReturn();
        return StaticSubject.getRefun() + nss.val() + Storage.feed + Storage.foo + nss.lift(80) + nss.lift(6);
    }

    public static void main(String[] args) throws Exception {
        new CompilationUnitWorker().transform("./src/test/java/TestTransformer/MockTest.java");
    }
}
