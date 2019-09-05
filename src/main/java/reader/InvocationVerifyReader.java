package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;

public class InvocationVerifyReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("verify\\(([a-zA-Z0-9_$]+)(?:\\s*,\\s*(.*?))?\\)\\.([a-zA-Z0-9]+)\\((.*)\\)");
    
    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher vmp = MP.matcher(stm);
        if ( ! vmp.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String subject = vmp.group(1);
        String fact = vmp.group(2);
        fact = fact == null ? "atLeastOnce" : fact;
        String methodName = vmp.group(3);
        String param = vmp.group(4);

        CallMeta meta = new CallMeta(param, fact);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        craft.setCallMeta(meta);
        return new StatementPiece(VERIFY_STM).beWith(craft);
    }
}

