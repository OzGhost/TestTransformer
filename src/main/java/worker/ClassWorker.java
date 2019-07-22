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
        WoodLog.clear();
        WoodLog.reachClass(classUnit.getName().asString());
        for (MethodDeclaration methodUnit: classUnit.findAll(MethodDeclaration.class)) {
            new MethodWorker(this).transform(methodUnit);
        }
        nameLeadingFields.put("abc", "Number");
        nameLeadingFields.put("sql", "Date");
        NodeList<BodyDeclaration<?>> oldMembers = classUnit.getMembers();
        NodeList<BodyDeclaration<?>> mockedFields = declareMocks();
        NodeList<BodyDeclaration<?>> newMembers = new NodeList<>();

        newMembers.addAll(mockedFields);
        newMembers.addAll(oldMembers);
        classUnit.setMembers(newMembers);
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

    public String addField(String type) {
        String fieldName = NameUtil.createTypeBasedName(type, nameLeadingFields.keySet());
        nameLeadingFields.put(fieldName, type);
        if ( ! typeLeadingFields.containsKey(type)) {
            typeLeadingFields.put(type, fieldName);
        }
        return fieldName;
    }
}
