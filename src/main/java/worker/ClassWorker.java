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
    private Map<String, String> typeLeadingFields = new HashMap<>();

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
            List<VariableDeclarator> requiredFields = collectFieldsUsedByMethod(mockedFields, methodUnit);
            new MethodWorker(this).setRequiredFields(requiredFields).transform(methodUnit);
        }

        /*
        for (MethodDeclaration methodUnit: classUnit.findAll(MethodDeclaration.class)) {
            new MethodWorker(this).transform(methodUnit);
        }
        NodeList<BodyDeclaration<?>> oldMembers = classUnit.getMembers();
        NodeList<BodyDeclaration<?>> mockedFields = declareMocks();
        NodeList<BodyDeclaration<?>> newMembers = new NodeList<>();

        newMembers.addAll(mockedFields);
        newMembers.addAll(oldMembers);
        classUnit.setMembers(newMembers);
        */
    }

    private List<VariableDeclarator> collectFieldsUsedByMethod(List<VariableDeclarator> fields, MethodDeclaration methodUnit) {
        Set<String> usedName = new HashSet<>();
        for (NameExpr name: methodUnit.findAll(NameExpr.class)) {
            usedName.add(name.getName().asString());
        }
        List<VariableDeclarator> used = new ArrayList<>(fields.size());
        for (VariableDeclarator vari: fields) {
            String variName = vari.getName().asString();
            if (usedName.contains(variName)) {
                used.add(vari);
            }
        }
        return used;
    }

    private boolean isMockField(FieldDeclaration f) {
        for (AnnotationExpr fa: f.getAnnotations()) {
            if ( "Mock".equals(fa.getName().asString()) ) {
                return true;
            }
        }
        return false;
    }

    public NodeList<BodyDeclaration<?>> declareMocks() {
        NodeList<BodyDeclaration<?>> output = new NodeList<>();
        for (Map.Entry<String, String> field: nameLeadingFields.entrySet()) {
            NodeList<AnnotationExpr> annotations = new NodeList<>(new MarkerAnnotationExpr("Mocked"));
            NodeList<Modifier> modifiers = new NodeList<>(Modifier.privateModifier());
            Type fieldType = new ClassOrInterfaceType(field.getValue());
            String fieldName = field.getKey();
            NodeList<VariableDeclarator> variables = new NodeList<>(new VariableDeclarator(fieldType, fieldName));
            FieldDeclaration fd = new FieldDeclaration(modifiers, annotations, variables);
            output.add(fd);
        }
        return output;
    }

    public String getFieldNameByType(String type) {
        return typeLeadingFields.get(type);
    }

    public String getFieldTypeByName(String name) {
        return nameLeadingFields.get(name);
    }

    public String recordMock(String type) {
        String fieldName = NameUtil.createTypeBasedName(type, nameLeadingFields.keySet());
        recordMock(fieldName, type);
        return fieldName;
    }

    public void recordMock(String name, String type) {
        nameLeadingFields.put(name, type);
        if ( ! typeLeadingFields.containsKey(type)) {
            typeLeadingFields.put(type, name);
        }
    }

    public void recordMockIfAbsent(String type) {
        if ( ! typeLeadingFields.containsKey(type)) {
            recordMock(type);
        }
    }
}
