package worker;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;

public class ParameterMatchingWorker {

    public static NodeList<Expression> leach(String input) {
        System.out.println("found: '" + input + "'");
        if (input.isEmpty()) {
            return new NodeList<>();
        }
        String[] elements = input.split(",");
        NodeList<Expression> output = new NodeList<>();
        for (String el: elements) {
            output.add(elementLeach(el.trim()));
        }
        return output;
    }

    private static Expression elementLeach(String el) {
        Expression output = trySimpleTranslate(el);
        if (output != null) {
            return output;
        }
        return new NameExpr("anyString");
    }

    private static Expression trySimpleTranslate(String el) {
        return new NameExpr("anyInt");
    }
}
