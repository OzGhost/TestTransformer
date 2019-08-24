package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;

public class PrivateStaticReturnMockReader extends MockingReader {

    private static final Pattern P = Pattern.compile("when\\(([a-zA-Z0-9_$]+)\\.class,\\s*\"([a-zA-Z0-9_$]+)\"(?:\\s*,\\s*(.+))?\\)\\.thenReturn\\((.*)\\)");

    @Override
    public int read(String stm, Node node, Node belowNode) {
        Matcher mp = P.matcher(stm);
        if ( ! mp.find()) {
            return UNKNOW_STM;
        }
        String subject = mp.group(1);
        String methodName = mp.group(2);
        String param = mp.group(3);
        param = param == null ? "" : param;
        String out = mp.group(4);

        System.out.println(""+subject+":"+methodName+":"+param+":"+out);
        if (true) return UNKNOW_STM;

        Expression outExpr = null;
        try {
            outExpr = node.findFirst(MethodCallExpr.class)
                                    .get()
                                    .getArguments()
                                    .get(0);
        } catch(Exception e) {
            System.out.println("re: " + node);
            throw new RuntimeException(e);
        }

        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        CallMeta meta = new CallMeta(param, out, outExpr, false, false);
        craft.setCallMeta(meta);
        return MOCK_STM;
    }
}

