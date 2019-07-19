package worker;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;

public class ParameterMatchingWorker {
    
    public static NodeList<Parameter> leach(String input) {
        System.out.println("found: '" + input + "'");
        return new NodeList<>();
    }
}
