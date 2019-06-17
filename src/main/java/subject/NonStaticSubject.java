package subject;

public class NonStaticSubject {

    public static NonStaticSubject create() {
        return new NonStaticSubject();
    }

    public void noReturn(){
        Storage.foo = 105;
    }

    public int val() {
        return 29;
    }
}
