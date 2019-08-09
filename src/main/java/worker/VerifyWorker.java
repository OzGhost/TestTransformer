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

    private ParameterMatchingWorker paramWorker;

    private VerifyWorker(MethodWorker mlw) {
        paramWorker = ParameterMatchingWorker.forWorker(mlw);
    }

    public static VerifyWorker forWorker(MethodWorker mlw) {
        return new VerifyWorker(mlw);
    }

    public Statement transform(MockingMeta mockMeta) {
        return MockingMetaWrappingWorker.wrap(mockMeta, this::processVerifyingCraft, "Verifications");
    }

    private Statement[] processVerifyingCraft(Craft craft) {
        Statement[] output = new Statement[2];
        MethodCallExpr callExpr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
        callExpr.setArguments( paramWorker.leach( craft ) );
        output[0] = new ExpressionStmt(callExpr);
        AssignExpr countExpr = new AssignExpr(new NameExpr("times"), new IntegerLiteralExpr(1), AssignExpr.Operator.ASSIGN);
        output[1] = new ExpressionStmt(countExpr);
        return output;
    }
}
