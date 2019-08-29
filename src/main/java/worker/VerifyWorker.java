package worker;

import static meta.Name.*;
import meta.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class VerifyWorker {

    private static final Pattern TIMES_P = Pattern.compile("times\\(([a-zA-Z0-9_$]+)\\)");

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
        String fact = craft.getCallMeta().getFact();
        String verifyStrategy = "times";
        String verifyAspect = "1";
        if (fact == null || fact.isEmpty()) {
            WoodLog.attach(WARNING, "Found no fact for call: " + craft.getCallMeta());
        } else {
            if (fact.contains("atLeastOnce")) {
                verifyStrategy = "minTimes";
            } else if (fact.contains("never")) {
                verifyAspect = "0";
            } else {
                Matcher m = TIMES_P.matcher(fact);
                if (m.find()) {
                    verifyAspect = m.group(1);
                } else {
                    WoodLog.attach(WARNING, "Unsupported fact in call: " + craft.getCallMeta());
                }
            }
        }
        AssignExpr aspect = new AssignExpr(new NameExpr(verifyStrategy), new NameExpr(verifyAspect), AssignExpr.Operator.ASSIGN);
        output[1] = new ExpressionStmt(aspect);
        return output;
    }
}
