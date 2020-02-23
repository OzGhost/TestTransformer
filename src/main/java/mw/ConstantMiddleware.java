
package mw;

public class ConstantMiddleware {

    private static final String[] ori = new String[]{
        "MortgageConstants.GET_LOGGER_METHOD_NAME"
    };

    private static final String[] rep = new String[]{
        "\"getLogger\""
    };
    
    public static ConstantMiddleware i() {
        return new ConstantMiddleware();
    }

    public String hijack(String oStm) {
        String s = oStm;
        for (int i = 0; i < ori.length; ++i) {
            s = s.replace(ori[i], rep[i]);
        }
        return s;
    }
}

