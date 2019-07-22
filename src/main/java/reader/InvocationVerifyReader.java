package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;

public class InvocationVerifyReader extends MockingReader {

    private static final Pattern VERIFY_MP = Pattern.compile("verify\\(([^,]*),(.*)\\)\\.([^(]+)\\((.*)\\)");
    
    @Override
    public int read(String stm, Node node, Node belowNode) {
        Matcher verifyMp = VERIFY_MP.matcher(stm);
        if ( ! verifyMp.find()) {
            return UNKNOW_STM;
        }
        String subject = verifyMp.group(1);
        String fact = verifyMp.group(2);
        String methodName = verifyMp.group(3);
        String param = verifyMp.group(4);
        CallMeta meta = new CallMeta(param, fact);

        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        craft.setCallMeta(meta);
        return VERIFY_STM;
    }
}
