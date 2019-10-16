package reader;

import static meta.Name.*;
import meta.CallMeta;
import meta.Craft;

import worker.WoodLog;
import meta.StatementPiece;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.Node;

public class PrivateReturnMockReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("doReturn\\((.*?)\\)\\.when\\((.*?),\\s*\"(.*?)\"\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher m = MP.matcher(stm);
        if ( ! m.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String output = m.group(1);
        String subject = m.group(2);
        String methodName = m.group(3);

        Craft craft = new Craft();
        craft.setSubjectName(subject);
        craft.setMethodName(methodName);
        craft.setCallMeta(
            new CallMeta()
                .thenGive(output)
                .asAReturn( ReaderUtil.getDoReturnExpr(node) )
                .asPrivateCall()
        );
        return new StatementPiece(MOCK_STM).beWith(craft);
    }
}

