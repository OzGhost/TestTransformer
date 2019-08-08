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

    private ParameterMatchingWorker paramWorker;

    private MockWorker(MethodWorker mlw) {
        paramWorker = ParameterMatchingWorker.forWorker(mlw);
    }

    public static MockWorker forWorker(MethodWorker mlw) {
        return new MockWorker(mlw);
    }

    public Statement transform(MockingMeta mockingMeta) {
        return MockingMetaWrappingWorker.wrap(mockingMeta, this::processMockingCraft, "Expectations");
    }

    private Statement[] processMockingCraft(Craft craft) {
        CallMeta cm = craft.getCallMeta();
        if (cm.isRaise()) {
            System.out.println("Hit throw: " + cm.toString());
            return null;
        } else if (cm.isVoid()) {
            return null;
        } else {
            Statement[] output = new Statement[2];
            MethodCallExpr callExpr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
            callExpr.setArguments( paramWorker.leach(cm.getInput()) );
            output[0] = new ExpressionStmt(callExpr);
            AssignExpr returnExpr = new AssignExpr(new NameExpr("result"), cm.getOutputExpression(), AssignExpr.Operator.ASSIGN);
            output[1] = new ExpressionStmt(returnExpr);
            return output;
        }
    }
}

