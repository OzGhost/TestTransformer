package reader;

import static meta.Name.*;
import worker.WoodLog;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ReaderUtil {

    private ReaderUtil() {
        throw new UnsupportedOperationException();
    }

    public static Expression getOutputExpression(Node nodeWithThenReturnMethodCall) {
        for (MethodCallExpr m: nodeWithThenReturnMethodCall.findAll(MethodCallExpr.class)) {
            if ("thenReturn".equals(m.getName().asString())) {
                NodeList<Expression> args = m.getArguments();
                if (args.size() == 1) {
                    return args.get(0);
                }
            }
        }
        WoodLog.attach(WARNING, "Found no 'thenReturn' call in: "+nodeWithThenReturnMethodCall.toString());
        return null;
    }
}

