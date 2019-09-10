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
    private boolean _private;
    private String input;
    private String output;
    private Expression outputExpr;
    private String fact;

    public CallMeta() {
        _void = true;
    }

    public CallMeta(String param, String out, boolean isRaise, boolean isVoid) {
        input = param;
        _void = isVoid;
        _raise = isRaise;
        output = out;
    }

    public CallMeta(String param, String out, Expression outExpr, boolean isRaise, boolean isVoid) {
        this(param, out, isRaise, isVoid);
        outputExpr = outExpr;
    }

    public CallMeta take(String param) {
        input = param;
        return this;
    }

    public CallMeta thenGive(String outLiteral) {
        _void = false;
        output = outLiteral;
        return this;
    }

    public CallMeta thenGiveNothing() {
        _void = true;
        return this;
    }

    public CallMeta asAReturn(Expression outExp) {
        _raise = false;
        outputExpr = outExp;
        return this;
    }

    public CallMeta asAThrow(Expression throwExp) {
        _raise = true;
        outputExpr = throwExp;
        return this;
    }

    public CallMeta(String param, String truely) {
        input = param;
        fact = truely;
    }

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

    public String getFact() {
        return fact;
    }

    public boolean isPrivate() {
        return _private;
    }

    public CallMeta asPrivateCall() {
        _private = true;
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append(_private ? "p" : "")
            .append('[')
            .append(input)
            .append("] -> [")
            .append(
                    (fact == null)
                    ? _raise
                        ? "<throw> " + output
                        : _void
                            ? "<void>"
                            : output
                    : "<fact> " + fact
            )
            .append(']')
            .toString();
    }
}

