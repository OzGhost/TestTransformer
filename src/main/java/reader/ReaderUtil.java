package reader;

import static meta.Name.*;
import worker.WoodLog;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ReaderUtil {

    private static Pattern IC_EQ_P = Pattern.compile("assertEquals\\((\\d+)\\s*,\\s*[a-zA-Z0-9_$]+\\.times\\(\\)\\)");
    private static Pattern IC_THAT_P = Pattern.compile("assertThat\\([a-zA-Z0-9_$]+\\.times\\(\\)\\s*,\\s*[a-zA-Z0-9_$]*?\\.?(?:is|equalTo)\\((\\d+)\\)\\)");

    private ReaderUtil() {
        throw new UnsupportedOperationException();
    }

    public static Expression getReturnExpression(Node inputNode) {
        return getFirstArgumentExpr(inputNode, "thenReturn");
    }

    private static Expression getFirstArgumentExpr(Node inputNode, String methodName) {
        for (MethodCallExpr m: inputNode.findAll(MethodCallExpr.class)) {
            if (methodName.equals(m.getName().asString())) {
                NodeList<Expression> args = m.getArguments();
                if (args.size() == 1) {
                    return args.get(0);
                }
            }
        }
        WoodLog.attach(WARNING, "Found no '"+methodName+"' phase in: "+inputNode.toString());
        return null;
    }

    public static Expression getThrowExpression(Node inputNode) {
        return getFirstArgumentExpr(inputNode, "thenThrow");
    }

    public static int getICExpectedTimes(String stm) {
        Matcher icm = IC_EQ_P.matcher(stm);
        if ( icm.find() ) {
            return Integer.parseInt(icm.group(1));
        }
        icm = IC_THAT_P.matcher(stm);
        if ( icm.find() ) {
            return Integer.parseInt(icm.group(1));
        }
        return -1;
    }

    public static <T extends Node> T findClosestParent(Node currentNode, Class<T> parentType) {
        Node target = null;
        Optional<Node> on = currentNode.getParentNode();
        while (on.isPresent()) {
            target = on.get();
            //System.out.println(target.getClass());
            if (target.getClass() == parentType) break;
            on = target.getParentNode();
        }
        if (target.getClass() == parentType) return parentType.cast(target);
        return null;
    }
}

