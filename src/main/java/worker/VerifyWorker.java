package worker;

import meta.*;
import java.util.*;
import java.util.function.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class VerifyWorker {

    private static final Function<Craft, Statement[]> VERIFY_PROCESSOR = new Function<Craft, Statement[]>() {
        @Override
        public Statement[] apply(Craft craft) {
            ParameterMatchingWorker.leach(craft.getCallMeta().getInput());
            Statement[] output = new Statement[2];
            Expression expr = null;
            expr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
            output[0] = new ExpressionStmt(expr);
            expr = new AssignExpr(new NameExpr("times"), new IntegerLiteralExpr(1), AssignExpr.Operator.ASSIGN);
            output[1] = new ExpressionStmt(expr);
            return output;
        }
    };

    public static Statement transform(MockingMeta mockMeta) {
        return MockingMetaWrappingWorker.wrap(mockMeta, VERIFY_PROCESSOR, "Verifications");
    }
}
