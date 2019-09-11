package reader;

import static meta.Name.*;
import worker.WoodLog;
import meta.StatementPiece;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.Node;

public class PrivateReturnMockReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("doReturn\\(.*?\\)\\.when\\(.*?,\\s*\"");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher m = MP.matcher(stm);
        if ( ! m.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        WoodLog.attach(ERROR, "Found forbidden mocking: private static/non-static return mock");
        return new StatementPiece(UNKNOW_STM);
    }
}

