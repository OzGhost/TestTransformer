
package storage;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.NodeList;

public class MethodDesc {
    private Type returnType;
    private String[][] paramTypes;
    private NodeList<Parameter> arguments;
    private NodeList<ReferenceType> exceptions;

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type rt) {
        returnType = rt;
    }

    public String[][] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(String[][] pts) {
        paramTypes = pts;
    }

    public NodeList<Parameter> getArguments() {
        return arguments;
    }

    public void setArguments(NodeList<Parameter> args) {
        arguments = args;
    }

    public NodeList<ReferenceType> getExceptions() {
        return exceptions;
    }

    public void setExceptions(NodeList<ReferenceType> exs) {
        exceptions = exs;
    }
}

