package meta;

import worker.WoodLog;
import com.github.javaparser.ast.expr.Expression;

public class CallMeta {

    public static final CallMeta NIL = new CallMeta(){
        @Override
        public String toString() {
            return "NIL";
        }
    };

    private boolean _void;
    private boolean _raise;
    private String input;
    private String output;
    private String cause;
    private Expression outputExpr;
    private String fact;
    private boolean isPrivate;

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

    public CallMeta(String param, String out, Expression outExpr, boolean isRaise, boolean isVoid) {
        this(param, out, isRaise, isVoid);
        outputExpr = outExpr;
    }

    public CallMeta(String param, String truely) {
        input = param;
        fact = truely;
        WoodLog.facts.add(truely.trim());
    }

    private CallMeta() {}

    public Expression getOutputExpression() {
        return outputExpr;
    }

    public boolean isVoid() {
        return _void;
    }

    public boolean isRaise() {
        return _raise;
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

    public String getFact() {
        return fact;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public CallMeta asPrivateCall() {
        isPrivate = true;
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append(isPrivate ? "p" : "")
            .append('[')
            .append(input)
            .append("] -> [")
            .append(
                    (fact == null)
                    ? _raise
                        ? "<throw> " + cause
                        : _void
                            ? "<void>"
                            : output
                    : "<fact> " + fact
            )
            .append(']')
            .toString();
    }
}

