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

    public static Statement wrapMockingStatement(NodeList<Statement> mockingStmts, String wrapperName) {
        BlockStmt bodyBlock = new BlockStmt(mockingStmts);
        InitializerDeclaration initBlock = new InitializerDeclaration(false, bodyBlock);
        NodeList<BodyDeclaration<?>> initBlockAsList = new NodeList<>();
        initBlockAsList.add(initBlock);
        ObjectCreationExpr expectBlock = new ObjectCreationExpr(
                null,
                new ClassOrInterfaceType(null, wrapperName),
                new NodeList<>(),
                new NodeList<>(),
                initBlockAsList
        );
        return new ExpressionStmt(expectBlock);
    }
}

