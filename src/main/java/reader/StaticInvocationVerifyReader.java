package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import worker.WoodLog;

public class StaticInvocationVerifyReader extends MockingReader {
    private static final Pattern VERIFY_STATIC_MP = Pattern.compile("verifyStatic\\(([a-zA-Z0-9_$]+)\\.class,(.+)\\)");
    private static final Pattern VERIFY_STATIC_WITHOUT_FACT_MP = Pattern.compile("verifyStatic\\(([a-zA-Z0-9_$]+)\\.class\\)");
    
    @Override
    public int read(String stm, Node node, Node belowNode) {
        boolean withoutFact = true;
        Matcher verifyStaticMp = VERIFY_STATIC_WITHOUT_FACT_MP.matcher(stm);
        if ( ! verifyStaticMp.find()) {
            verifyStaticMp = VERIFY_STATIC_MP.matcher(stm);
            if (verifyStaticMp.find()) {
                withoutFact = false;
            } else {
                return UNKNOW_STM;
            }
        }
        String subject = verifyStaticMp.group(1);
        if (belowNode == null) {
            WoodLog.attach(ERROR, subject, "Found no recall for ["+stm+"]");
            return VERIFY_STM;
        }
        String fact = "atLeastOnce";
        if ( ! withoutFact) {
            fact = verifyStaticMp.group(2);
        }
        String staticRecallPattern = subject + STATIC_RECALL_PATTERN_SUFFIX;
        Matcher recallMatcher = Pattern.compile(staticRecallPattern).matcher(belowNode.toString());
        if (recallMatcher.find()) {
            String methodName = recallMatcher.group(1);
            String param = recallMatcher.group(2);
            CallMeta meta = new CallMeta(param, fact);
            
            craft.setSubjectName(subject);
            craft.setMethodName(methodName);
            craft.setCallMeta(meta);
            return FOLLOWED_VERIFY_STM;
        }
        WoodLog.attach(ERROR, subject, "Found no recall in ["+belowNode.toString()+"] for ["+stm+"]");
        return VERIFY_STM;
    }
}
