package worker;

import meta.*;
import java.util.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class VerifyWorker {

    public static Statement transform(MockingMeta mockMeta) {
        NodeList<Statement> verifications = new NodeList<>();
        Expression expr = null;
        for (Map.Entry<String, SubjectMeta> meta: mockMeta.getSubjectMetas().entrySet()) {
            String subjectName = meta.getKey();
            SubjectMeta subjectMeta = meta.getValue();
            for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMetas().entrySet()) {
                String methodName = mm.getKey();
                List<CallMeta> callMetas = mm.getValue();
                for (CallMeta cm: callMetas) {
                    expr = new MethodCallExpr(new NameExpr(subjectName), methodName);
                    verifications.add(new ExpressionStmt(expr));
                    expr = new AssignExpr(new NameExpr("times"), new IntegerLiteralExpr(1), AssignExpr.Operator.ASSIGN);
                    verifications.add(new ExpressionStmt(expr));
                }
            }
        }
        return wrapMockStatement(verifications);
    }

    private static Statement wrapMockStatement(NodeList<Statement> mockStmts) {
        BlockStmt bodyBlock = new BlockStmt(mockStmts);
        InitializerDeclaration initBlock = new InitializerDeclaration(false, bodyBlock);
        NodeList<BodyDeclaration<?>> initBlockAsList = new NodeList<>();
        initBlockAsList.add(initBlock);
        ObjectCreationExpr expectBlock = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType("Verifications"),
                new NodeList<>(),
                new NodeList<>(),
                initBlockAsList
                );
        return new ExpressionStmt(expectBlock);
    }
}
