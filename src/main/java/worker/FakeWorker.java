package worker;

import java.util.List;
import java.util.LinkedList;
import java.util.Map.Entry;

import meta.MockingMeta;
import meta.SubjectMeta;
import meta.CallMeta;

import storage.CodeBaseStorage;
import storage.MethodDesc;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

public class FakeWorker {

    public static List<Statement> transform(MockingMeta records) {
        List<Statement> expectations = new LinkedList<>();
        for (Entry<String, SubjectMeta> subjectEntry: records) {
            SubjectMeta sm = subjectEntry.getValue();
            expectations.add( rewriteClass(subjectEntry.getKey(), sm) );
        }
        return expectations;
    }

    private static Statement rewriteClass(String className, SubjectMeta meta) {
        NodeList<BodyDeclaration<?>> body = new NodeList<>();
        for (Entry<String, List<CallMeta>> methodEntry: meta) {
            body.add( rewriteMethod(className, meta.getPkg(), methodEntry.getKey(), methodEntry.getValue()) );
        }
        
        ObjectCreationExpr classMockUp = new ObjectCreationExpr();
        classMockUp.setType(new ClassOrInterfaceType(null, "MockUp<"+className+">"));
        classMockUp.setAnonymousClassBody(body);
        return new ExpressionStmt(classMockUp);
    }

    private static BodyDeclaration<?> rewriteMethod(String className, String pkg, String methodName, List<CallMeta> metas) {
        if (metas.size() > 1) {
            WoodLog.attach("Multiple scenario detected @@");
        }


        MethodDesc desc = CodeBaseStorage.findMethodDesc(
                new String[]{className, pkg},
                methodName,
                metas.get(0).getInput().split(",").length
        );
        
        MethodDeclaration method = new MethodDeclaration();
        method.setName(new SimpleName(methodName));
        method.setAnnotations(new NodeList<>( new MarkerAnnotationExpr("Mock") ));

        method.setType( desc.getReturnType() );
        method.setThrownExceptions( desc.getExceptions() );
        method.setParameters( desc.getArguments() );
        return method;
    }
}

