package subject;

import java.util.List;

public class NonStaticSubject {

    int x = 35;

    public NonStaticSubject() {}
    public NonStaticSubject(int x) {
        this.x = x;
    }

    public static NonStaticSubject create(int input, Long sp, List<String> ac) {
        return new NonStaticSubject();
    }

    public static int rand() {
        return 125;
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

    public int fval() {
        return pval();
    }

    private int pval() {
        return 22;
    }

    public int sval() {
        return x;
    }

    public void reset() {
        throw new RuntimeException("No you");
    }

    public static int doNoop() {
        return 12;
    }
}
