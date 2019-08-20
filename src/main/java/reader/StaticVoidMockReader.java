package reader;

import static meta.Name.*;
import meta.*;
import com.github.javaparser.ast.*;
import java.util.regex.*;
import worker.WoodLog;

public class StaticVoidMockReader extends MockingReader {
    
    private static final Pattern STATIC_VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\(([a-zA-Z\\d]+)\\.class\\)");

    @Override
    public int read(String stm, Node node, Node belowNode) {
        Matcher staticVoidMp = STATIC_VOID_MP.matcher(stm);
        if ( ! staticVoidMp.find()) {
            return UNKNOW_STM;
        }
        String subject = staticVoidMp.group(1);
        if (belowNode == null) {
            WoodLog.attach(ERROR, subject, "Found no recall for <"+stm+">");
            return MOCK_STM;
        }
        String recallPattern = subject + STATIC_RECALL_PATTERN_SUFFIX;
        Matcher staticVoidFollowMp = Pattern.compile(recallPattern).matcher(belowNode.toString());
        if (staticVoidFollowMp.find()) {
            String call = staticVoidFollowMp.group(1);
            String param = staticVoidFollowMp.group(2);
            CallMeta meta = new CallMeta(param, "", false, true);

            craft.setSubjectName(subject);
            craft.setMethodName(call);
            craft.setCallMeta(meta);

            return FOLLOWED_MOCK_STM;
        }
        WoodLog.attach(ERROR, subject, "Found no recall in <" + belowNode.toString() + "> for <"+stm+">");
        return MOCK_STM;
    }
}

