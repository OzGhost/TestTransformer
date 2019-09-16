package reader;

import static meta.Name.*;
import meta.*;
import com.github.javaparser.ast.*;
import java.util.regex.*;

public class VoidMockReader extends MockingReader {
    
    private static final Pattern VOID_MP = Pattern.compile("doNothing\\(\\)\\.when\\(([a-zA-Z0-9_$]+)\\)\\.([^\\(]+)\\((.*)\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher voidMp = VOID_MP.matcher(stm);
        if ( ! voidMp.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String subject = voidMp.group(1);
        String call = voidMp.group(2);
        String param = voidMp.group(3);

        CallMeta meta = new CallMeta(param, "", false, true);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(call);
        craft.setCallMeta(meta);
        return new StatementPiece(MOCK_STM).beWith(craft);
    }
}
