package reader;

import static meta.Name.*;
import meta.*;
import com.github.javaparser.ast.*;
import java.util.regex.*;

public class VoidMockReader extends MockingReader {
    
    private static final Pattern VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\((.+)\\)\\.([^\\(]+)\\((.*)\\)");

    @Override
    public int read(String stm, Node node, Node belowNode) {
        Matcher voidMp = VOID_MP.matcher(stm);
        if ( ! voidMp.find()) {
            return UNKNOW_STM;
        }
        String subject = voidMp.group(1);
        String call = voidMp.group(2);
        String param = voidMp.group(3);

        CallMeta meta = new CallMeta(param, "", false, true);

        craft.setSubjectName(subject);
        craft.setMethodName(call);
        craft.setCallMeta(meta);
        return MOCK_STM;
    }
}
