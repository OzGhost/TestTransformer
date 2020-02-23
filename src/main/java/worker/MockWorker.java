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

    public Statement transform(List<Craft> crafts) {
        NodeList<Statement> mockStms = new NodeList<>();
        for (Craft craft: crafts) {
            Statement[] stms = processMockingCraft(craft);
            for (Statement stm: processMockingCraft(craft)) {
                mockStms.add(stm);
            }
        }
        return MockingMetaWrappingWorker.wrapMockingStatement(mockStms, "Expectations");
    }

    private Statement[] processMockingCraft(Craft craft) {
        CallMeta cm = craft.getCallMeta();
        String subjectName = craft.getSubjectName();
        if (subjectName.contains(".") && subjectName.contains(")")) {
            subjectName = "\"" + subjectName + "\"";
        }
        MethodCallExpr callExpr = new MethodCallExpr(new NameExpr(subjectName), craft.getMethodName());
        callExpr.setArguments( paramWorker.leach( craft ) );
        Statement actionReplay = new ExpressionStmt(callExpr);
        if (cm.isVoid()) {
            // have no result phase
            return new Statement[]{actionReplay};
        } else {
            Statement[] o = relayOutputs(cm.getOutputExprs());
            o[0] = actionReplay;
            return o;
        }
    }

    private Statement[] relayOutputs(NodeList<Expression> exprs) {
        Statement[] outputs = new Statement[exprs.size() + 1];
        int index = 1;
        for (Expression expr: exprs) {
            AssignExpr outExpr = new AssignExpr(new NameExpr("result"), expr, AssignExpr.Operator.ASSIGN);
            outputs[index++] = new ExpressionStmt(outExpr);
        }
        return outputs;
    }
}

