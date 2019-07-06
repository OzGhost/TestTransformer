package worker;

import java.util.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

public class ClassWorker {

    private Map<String, String> fields = new HashMap<>();

    public void transform(ClassOrInterfaceDeclaration classUnit) {
        for (MethodDeclaration methodUnit: classUnit.findAll(MethodDeclaration.class)) {
            new MethodWorker(this).transform(methodUnit);
        }
    }

    public String typeOf(String varName) {
        return fields.get(varName);
    }
}
