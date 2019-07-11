package worker;

import static meta.Name.*;
import meta.*;
import java.util.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;
import com.google.common.collect.ImmutableMap;

public class StaticMockWorker {

    private static final ImmutableMap<Class<? extends Expression>, Type> EXPR_TO_TYPE =
        new ImmutableMap.Builder<Class<? extends Expression>, Type>()
            .put(IntegerLiteralExpr.class, new PrimitiveType(PrimitiveType.Primitive.INT))
            .put(CharLiteralExpr.class, new PrimitiveType(PrimitiveType.Primitive.CHAR))
            .put(DoubleLiteralExpr.class, new PrimitiveType(PrimitiveType.Primitive.DOUBLE))
            .put(LongLiteralExpr.class, new PrimitiveType(PrimitiveType.Primitive.LONG))
            .put(StringLiteralExpr.class, new ClassOrInterfaceType("String"))
            .build();

    public static Statement transform(Map.Entry<String, SubjectMockMeta> smm, Map<String, Type> varTypeMap) {
        NodeList<BodyDeclaration<?>> mockMethods = new NodeList<>();
        String subjectName = smm.getKey();
        WoodLog.reachSubject(subjectName);
        SubjectMockMeta subjectMeta = smm.getValue();
        for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMockMetas().entrySet()) {
            String methodName = mm.getKey();
            WoodLog.reachMethod(methodName);
            List<CallMeta> callMetas = mm.getValue();
            //WoodLog.attach(1, subjectName, methodName, callMetas.get(0), "test for fun");
            for (CallMeta cm: callMetas) {
                if (cm.isRaise()) {
                    WoodLog.attach(ERROR, cm, "Haven't support throw mocking for static call yet");
                } else {
                    MarkerAnnotationExpr annotation = new MarkerAnnotationExpr("Mock");
                    MethodDeclaration md = new MethodDeclaration(
                        new NodeList<>(), // modifiers
                        new NodeList<>(annotation), // annotations
                        new NodeList<>(), // typeParameters
                        findReturnType(cm, varTypeMap), // return type
                        new SimpleName(methodName), // name
                        new NodeList<>(), // parameters
                        new NodeList<>(), // thrownExceptions
                        buildBodyBlock(cm)
                    );
                    mockMethods.add(md);
                }
            }
        }

        NodeList<Type> parameterized = new NodeList<>();
        parameterized.add( new ClassOrInterfaceType(subjectName) );
        ClassOrInterfaceType name = new ClassOrInterfaceType(
            null,
            new SimpleName("MockUp"),
            parameterized
        );
        ObjectCreationExpr smock = new ObjectCreationExpr(
            null,
            name,
            new NodeList<>(),
            new NodeList<>(),
            mockMethods
        );
        return new ExpressionStmt(smock);
    }

    private static BlockStmt buildBodyBlock(CallMeta cm) {
        if (cm.isVoid()) {
            return new BlockStmt();
        }
        ReturnStmt returnStm = new ReturnStmt(cm.getOutput());
        NodeList<Statement> bodyStmts = new NodeList<>(returnStm);
        return new BlockStmt(bodyStmts);
    }

    private static Type findReturnType(CallMeta cm, Map<String, Type> varTypeMap) {
        if (cm.isVoid()) {
            return new VoidType();
        }
        Type type = EXPR_TO_TYPE.get(cm.getOutputExpression().getClass());
        if (type != null) {
            return type;
        }
        Type returnType = varTypeMap.get(cm.getOutput());
        if (returnType == null) {
            WoodLog.attach(ERROR, cm, "Cannot detect return type");
            return new PrimitiveType(PrimitiveType.Primitive.INT);
        }
        return returnType;
    }
}

