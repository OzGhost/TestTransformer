package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;

public class InvocationVerifyReader extends MockingReader {

    private static final Pattern VERIFY_MP = Pattern.compile("verify\\(([^,]*),(.*)\\)\\.([^(]+)\\((.*)\\)");
    private static final Pattern VERIFY_WITHOUT_FACT_MP = Pattern.compile("verify\\(([a-zA-Z0-9_$]+)\\)\\.([^(]+)\\((.*)\\)");
    
    @Override
    public int read(String stm, Node node, Node belowNode) {
        boolean withoutFact = true;
        Matcher verifyMp = VERIFY_WITHOUT_FACT_MP.matcher(stm);
        if ( ! verifyMp.find()) {
            verifyMp = VERIFY_MP.matcher(stm);
            if (verifyMp.find()) {
                withoutFact = false;
            } else {
                return UNKNOW_STM;
            }
        }
        String subject = verifyMp.group(1);
        String methodName = "";
        String param = "";
        String fact = "";
        if (withoutFact) {
            fact = "atLeastOnce";
            methodName = verifyMp.group(2);
            param = verifyMp.group(3);
        } else {
            fact = verifyMp.group(2);
            methodName = verifyMp.group(3);
            param = verifyMp.group(4);
        }
        CallMeta meta = new CallMeta(param, fact);

        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        craft.setCallMeta(meta);
        return VERIFY_STM;
    }
}

