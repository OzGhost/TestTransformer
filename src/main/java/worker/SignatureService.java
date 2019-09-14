package worker;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class SignatureService {

    public static String extractSignature(MethodDeclaration mUnit) {
        return mUnit.getName().asString() + ":" + mUnit.getParameters().size();
    }

    public static String extractSignature(MethodCallExpr mExpr) {
        return mExpr.getName().asString() + ":" + mExpr.getArguments().size();
    }
}

