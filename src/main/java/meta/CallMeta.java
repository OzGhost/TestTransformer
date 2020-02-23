package meta;

import worker.WoodLog;
import com.github.javaparser.ast.NodeList;
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
    private NodeList<Expression> outputExprs;
    private String fact;

    public CallMeta() {
        _void = true;
        input = "";
    }

    public CallMeta(String param, String out, boolean isRaise, boolean isVoid) {
        input = param;
        _void = isVoid;
        _raise = isRaise;
        output = out;
    }

    public CallMeta(String param, String out, NodeList<Expression> outExprs, boolean isRaise, boolean isVoid) {
        this(param, out, isRaise, isVoid);
        outputExprs = outExprs;
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

    public CallMeta asAReturn(NodeList<Expression> outExps) {
        _raise = false;
        outputExprs = outExps;
        return this;
    }

    public CallMeta asAThrow(NodeList<Expression> throwExps) {
        _raise = true;
        outputExprs = throwExps;
        return this;
    }

    public CallMeta(String param, String truely) {
        input = param;
        fact = truely;
    }

    public NodeList<Expression> getOutputExprs() {
        return outputExprs;
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

