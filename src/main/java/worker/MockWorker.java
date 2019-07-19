package worker;

import meta.*;
import java.util.*;
import java.util.function.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class MockWorker {

    private static final Function<Craft, Statement[]> MOCK_PROCESSOR = new Function<Craft, Statement[]>() {
        @Override
        public Statement[] apply(Craft craft) {
            CallMeta cm = craft.getCallMeta();
            ParameterMatchingWorker.leach(cm.getInput());
            if (cm.isRaise()) {
                System.out.println("Hit throw: " + cm.toString());
                return null;
            } else if (cm.isVoid()) {
                return null;
            } else {
                Statement[] output = new Statement[2];
                Expression expr = null;
                expr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
                output[0] = new ExpressionStmt(expr);
                expr = new AssignExpr(new NameExpr("result"), cm.getOutputExpression(), AssignExpr.Operator.ASSIGN);
                output[1] = new ExpressionStmt(expr);
                return output;
            }
        }
    };

    public static Statement transform(MockingMeta mockingMeta) {
        return MockingMetaWrappingWorker.wrap(mockingMeta, MOCK_PROCESSOR, "Expectations");
    }
}

