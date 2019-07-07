package meta;

public class CoreMockMeta {

    public static final CoreMockMeta NIL = new CoreMockMeta(){
        @Override
        public String toString() {
            return "CoreMockMeta.NIL";
        }
    };

    private String input;
    private boolean _void;
    private String output;
    private String raise;

    public CoreMockMeta(String param, String out, boolean isRaise, boolean isVoid) {
        input = param;
        _void = isVoid;
        if (isRaise) {
            raise = out;
        } else {
            output = out;
        }
    }

    private CoreMockMeta() {}

    public boolean isVoid() {
        return _void;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public String getRaise() {
        return raise;
    }
}

