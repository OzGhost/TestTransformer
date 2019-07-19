package worker;

import meta.*;
import java.util.*;
import java.util.function.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class MockingMetaWrappingWorker {

    public static Statement wrap(MockingMeta mockMeta, Function<Craft, Statement[]> craftProcessor, String wrapperName) {
        Craft travelCraft = new Craft();
        NodeList<Statement> mockingStmts = new NodeList<>();
        Expression expr = null;
        for (Map.Entry<String, SubjectMeta> meta: mockMeta.getSubjectMetas().entrySet()) {
            travelCraft.setSubjectName(meta.getKey());
            SubjectMeta subjectMeta = meta.getValue();
            for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMetas().entrySet()) {
                travelCraft.setMethodName(mm.getKey());
                List<CallMeta> callMetas = mm.getValue();
                for (CallMeta cm: callMetas) {
                    travelCraft.setCallMeta(cm);
                    Statement[] crafted = craftProcessor.apply(travelCraft);
                    if (crafted != null) {
                        mockingStmts.add(crafted[0]);
                        mockingStmts.add(crafted[1]);
                    }
                }
            }
        }
        return wrapMockStatement(mockingStmts, wrapperName);
    }

    private static Statement wrapMockStatement(NodeList<Statement> mockingStmts, String wrapperName) {
        BlockStmt bodyBlock = new BlockStmt(mockingStmts);
        InitializerDeclaration initBlock = new InitializerDeclaration(false, bodyBlock);
        NodeList<BodyDeclaration<?>> initBlockAsList = new NodeList<>();
        initBlockAsList.add(initBlock);
        ObjectCreationExpr expectBlock = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType(wrapperName),
                new NodeList<>(),
                new NodeList<>(),
                initBlockAsList
        );
        return new ExpressionStmt(expectBlock);
    }
}

