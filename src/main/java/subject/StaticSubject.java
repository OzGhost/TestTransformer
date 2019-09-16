package subject;

public class StaticSubject {

  public static void noRefun() {
    Storage.feed = 100;
  }

  public static int getRefun() {
    return 189;
  }

  public static int rebase(int input) {
      if (input < 10) {
          return -1;
      }
      return 1;
  }

  public static int fval() throws Exception {
      return pval();
  }

  private static int pval() throws Exception {
      return 98;
  }

  public static NonStaticSubject getNext() {
      return new NonStaticSubject();
  }

  public static int fn05() {
      return fn05_private();
  }

  public static int fn05_private() {
      return 15;
  }

  public static void fn06() {
      fn06_private();
  }

  private static void fn06_private() {
      throw new RuntimeException("interrupted");
  }

  public static int doNoop() {
      return 28;
  }
}
