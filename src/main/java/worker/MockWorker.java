package worker;

import meta.*;
import java.util.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class MockWorker {

    public static Statement transform(MockMeta mockMeta) {
        NodeList<Statement> expectations = new NodeList<>();
        Expression expr = null;
        for (Map.Entry<String, SubjectMockMeta> meta: mockMeta.getSubjectMockMetas().entrySet()) {
            String subjectName = meta.getKey();
            SubjectMockMeta subjectMeta = meta.getValue();
            for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMockMetas().entrySet()) {
                String methodName = mm.getKey();
                List<CallMeta> callMetas = mm.getValue();
                for (CallMeta cm: callMetas) {
                    if (cm.isRaise()) {
                        System.out.println("Hit throw: " + cm.toString());
                    } else if (cm.isVoid()) {
                        //TODO: special handle for void function if needed
                    } else {
                        expr = new MethodCallExpr(new NameExpr(subjectName), methodName);
                        expectations.add(new ExpressionStmt(expr));
                        expr = new AssignExpr(new NameExpr("result"), cm.getOutputExpression(), AssignExpr.Operator.ASSIGN);
                        expectations.add(new ExpressionStmt(expr));
                    }
                }
            }
        }
        return wrapMockStatement(expectations);
    }

    private static Statement wrapMockStatement(NodeList<Statement> mockStmts) {
        BlockStmt bodyBlock = new BlockStmt(mockStmts);
        InitializerDeclaration initBlock = new InitializerDeclaration(false, bodyBlock);
        NodeList<BodyDeclaration<?>> initBlockAsList = new NodeList<>();
        initBlockAsList.add(initBlock);
        ObjectCreationExpr expectBlock = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType("Expectations"),
                new NodeList<>(),
                new NodeList<>(),
                initBlockAsList
        );
        return new ExpressionStmt(expectBlock);
    }
}

