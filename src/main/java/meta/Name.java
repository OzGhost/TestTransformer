package meta;

public class Name {
    public static final int INFO = 1011;
    public static final int WARNING = 1012;
    public static final int ERROR = 1013;

    public static final int UNKNOW_STM = 3040;
    public static final int NORMAL_STM = 3041;
    public static final int MOCK_STM = 3042;
    public static final int FOLLOWED_MOCK_STM = 3043;
    public static final int VERIFY_STM = 3044;
    public static final int FOLLOWED_VERIFY_STM = 3045;
    public static final int NEW_INSTANT_INJECTION = 3046;

    public static final String CLASS_FIELD = "SRC: field of class level";
    public static final String MOCKED_INSTANCE = "SRC: mocked instance declaration";
    public static final String DECLARED_INSTANCE = "SRC: declared instance inside method";
    public static final String STATIC_INVOCATION = "SRC: static invocation mock";

    public static final String INTERRUPT_SIGNAL = "SIGKIL";
}
