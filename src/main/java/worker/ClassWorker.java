package worker;

import java.util.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

public class ClassWorker {

    private Map<String, String> nameLeadingFields = new LinkedHashMap<>();

    public void transform(ClassOrInterfaceDeclaration classUnit) {
        WoodLog.reachClass(classUnit.getName().asString());
        ParameterMatchingWorker.registerClassLevelWorker(this);

        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();

        for (BodyDeclaration<?> declaration: classUnit.getMembers()) {
            if (declaration instanceof FieldDeclaration) {
                fields.add( (FieldDeclaration) declaration);
            } else if (declaration instanceof MethodDeclaration){
                methods.add( (MethodDeclaration) declaration);
            }
        }

        List<VariableDeclarator> mockedFields = new ArrayList<>();
        for (FieldDeclaration fieldUnit: fields) {
            if ( isMockField(fieldUnit) ) {
                mockedFields.addAll(fieldUnit.getVariables());
                fieldUnit.remove();
            }
        }

        for (MethodDeclaration methodUnit: methods) {
            new MethodWorker(methodUnit).setRequiredFields(mockedFields).transform();
        }
    }

    private boolean isMockField(FieldDeclaration f) {
        for (AnnotationExpr fa: f.getAnnotations()) {
            if ( "Mock".equals(fa.getName().asString()) ) {
                return true;
            }
        }
        return false;
    }
}

