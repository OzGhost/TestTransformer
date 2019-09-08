package worker;

import static meta.Name.ERROR;
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

    public Statement transform(MockingMeta mockingMeta, Set<String> pmv) {
        NodeList<Statement> mockStms = new NodeList<>();
        for (Craft craft: mockingMeta.toCrafts()) {
            Statement[] stms = processMockingCraft(craft, pmv);
            if (stms == null) continue;
            mockStms.add(stms[0]);
            if (stms[1] != null) {
                mockStms.add(stms[1]);
            }
        }
        return MockingMetaWrappingWorker.wrapMockingStatement(mockStms, "Expectations", pmv.toArray(new String[pmv.size()]));
    }

    private Statement[] processMockingCraft(Craft craft, Set<String> pmv) {
        CallMeta cm = craft.getCallMeta();
        Statement[] output = new Statement[2];
        //if (cm.isVoid()) return null;
        MethodCallExpr callExpr = new MethodCallExpr(new NameExpr(craft.getSubjectName()), craft.getMethodName());
        callExpr.setArguments( paramWorker.leach( craft ) );
        output[0] = new ExpressionStmt(callExpr);
        if (cm.isVoid()) {
            if (pmv.contains(craft.getSubjectName())) {
                // have no result phase
            }
        } else {
            AssignExpr outExpr = new AssignExpr(new NameExpr("result"), cm.getOutputExpression(), AssignExpr.Operator.ASSIGN);
            output[1] = new ExpressionStmt(outExpr);
        }
        return output;
    }
}

