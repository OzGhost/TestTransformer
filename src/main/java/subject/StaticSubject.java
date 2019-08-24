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
}
