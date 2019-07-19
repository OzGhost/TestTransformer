package reader;

import static meta.Name.*;
import java.util.regex.*;

public class ReturnMockReader extends MockingReader {

    private static final Pattern RETURNABLE_MP = Pattern.compile("when\\((.+)\\.([^\\(]+)\\((.*)\\)\\)\\.thenReturn\\((.+)\\)");

    @Override
    public int read(String stm, Node node, Node belowNode) {
        Matcher returnMp = RETURNABLE_MP.matcher(nodeAsString);
        if ( ! returnMp.find()) {
            return UNKNOW_STM;
        }
        String subject = returnMp.group(1);
        String call = returnMp.group(2);
        String param = returnMp.group(3);
        String out = returnMp.group(4);

        Expression outExpr = node.findFirst(MethodCallExpr.class)
                                    .get()
                                    .getArguments()
                                    .get(0);

        craft.setSubjectName(subject);
        craft.setMethodName(call);
        CallMeta meta = new CallMeta(param, out, outExpr, false, false);
        craft.setCallMeta(meta);
        return MOCK_STM;
    }
}