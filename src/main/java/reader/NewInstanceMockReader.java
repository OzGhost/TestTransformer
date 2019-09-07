package reader;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import meta.StatementPiece;
import static meta.Name.*;
import com.github.javaparser.ast.Node;

public class NewInstanceMockReader extends MockingReader {

    private static final Pattern MP = Pattern.compile("whenNew\\((.*)\\.class\\)");

    @Override
    public StatementPiece read(String stm, Node node, Node belowNode) {
        Matcher m = MP.matcher(stm);
        if ( ! m.find()) {
            return new StatementPiece(UNKNOW_STM);
        }
        String instanceType = m.group(1);
        return new StatementPiece(MOCK_STM).requestMock(instanceType).asRawType(NEW_INSTANT_INJECTION);
    }
}

