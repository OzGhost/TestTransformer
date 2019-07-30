package subject;

import java.util.List;

public class NonStaticSubject {

    public static NonStaticSubject create(int input, Long sp, List<String> ac) {
        return new NonStaticSubject();
    }

    public void noReturn(){
        Storage.foo = 105;
    }

    public int val() {
        return 29;
    }

    public int lift(int state) {
        if (state < 100) {
            return 99;
        }
        return 150;
    }
}
