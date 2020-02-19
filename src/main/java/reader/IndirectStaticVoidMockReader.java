package reader;

import static meta.Name.*;

import meta.CallMeta;
import meta.Craft;
import meta.StatementPiece;
import worker.WoodLog;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.Node;

public class IndirectStaticVoidMockReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("doNothing\\(\\)\\.when\\((.*?).class\\s*,\\s*\"([a-zA-Z0-9_$]+)\"\\s*,?\\s*(.*)\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher m = MP.matcher(stm);
        if ( ! m.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String subject = m.group(1);
        String methodName = m.group(2);
        String param = m.group(3);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        CallMeta meta = new CallMeta()
            .take(param)
            .thenGiveNothing();
        craft.setCallMeta(meta);
        return new StatementPiece(MOCK_STM).beWith(craft);
    }
}

