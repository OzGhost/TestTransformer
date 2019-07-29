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
            Statement[] output = new Statement[2];
            MethodCallExpr callExpr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
            callExpr.setArguments( ParameterMatchingWorker.leach(craft.getCallMeta().getInput()) );
            output[0] = new ExpressionStmt(callExpr);
            AssignExpr countExpr = new AssignExpr(new NameExpr("times"), new IntegerLiteralExpr(1), AssignExpr.Operator.ASSIGN);
            output[1] = new ExpressionStmt(countExpr);
            return output;
        }
    };

    public static Statement transform(MockingMeta mockMeta) {
        return MockingMetaWrappingWorker.wrap(mockMeta, VERIFY_PROCESSOR, "Verifications");
    }
}
