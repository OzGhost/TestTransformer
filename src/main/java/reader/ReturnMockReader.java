package reader;

import static meta.Name.*;
import java.util.regex.*;
import meta.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;

public class ReturnMockReader extends MockingReader {

    private static final Pattern RETURNABLE_MP = Pattern.compile("when\\(([^\\.]+)\\.([^\\(]+)\\((.*)\\)\\)\\.thenReturn\\((.+)\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher returnMp = RETURNABLE_MP.matcher(stm);
        if ( ! returnMp.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String subject = returnMp.group(1);
        String call = returnMp.group(2);
        String param = returnMp.group(3);
        String out = returnMp.group(4);

        Expression outExpr = ReaderUtil.getThenReturnExpr(node);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(call);
        CallMeta meta = new CallMeta(param, out, outExpr, false, false);
        craft.setCallMeta(meta);
        return new StatementPiece(MOCK_STM).beWith(craft);
    }
}

