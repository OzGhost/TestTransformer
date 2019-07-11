package worker;

import meta.*;
import java.util.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.body.*;

public class StaticMockWorker {

    public static Statement transform(Map.Entry<String, SubjectMockMeta> smm) {
        NodeList<BodyDeclaration<?>> mockMethods = new NodeList<>();
        String subjectName = smm.getKey();
        SubjectMockMeta subjectMeta = smm.getValue();
        for (Map.Entry<String, List<CallMeta>> mm: subjectMeta.getMethodMockMetas().entrySet()) {
            String methodName = mm.getKey();
            List<CallMeta> callMetas = mm.getValue();
            WoodLog.attach(1, subjectName, methodName, callMetas.get(0), "test for fun");
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
}

