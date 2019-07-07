package meta;

public class CallMeta {

    public static final CallMeta NIL = new CallMeta(){
        @Override
        public String toString() {
            return "CallMeta.NIL";
        }
    };

    private boolean _void;
    private boolean _raise;
    private String input;
    private String output;
    private String cause;

    public CallMeta(String param, String out, boolean isRaise, boolean isVoid) {
        input = param;
        _void = isVoid;
        _raise = isRaise;
        if (isRaise) {
            cause = out;
        } else {
            output = out;
        }
    }

    private CallMeta() {}

    public boolean isVoid() {
        return _void;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    public String getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append(input)
            .append("] -> [")
            .append(_raise ? "<throw> "+cause : _void ? "<void>" : output)
            .append(']')
            .toString();
    }
}

