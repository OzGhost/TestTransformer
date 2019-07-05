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
}
