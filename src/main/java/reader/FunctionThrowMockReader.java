package reader;

import static meta.Name.*;
import meta.StatementPiece;
import meta.CallMeta;
import meta.Craft;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;

public class FunctionThrowMockReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("when\\(([a-zA-Z0-9_$]+)\\.([a-zA-Z0-9_$]+)\\((.*)\\)\\).thenThrow\\((.*)\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher m = MP.matcher(stm);
        if ( ! m.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String subject = m.group(1);
        String call = m.group(2);
        String param = m.group(3);
        String out = m.group(4);

        Expression throwExp = ReaderUtil.getThrowExpression(node);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(call);
        CallMeta meta = new CallMeta().take(param).thenGive(out).asAThrow(throwExp);
        craft.setCallMeta(meta);
        return new StatementPiece(MOCK_STM).beWith(craft);
    }
}

