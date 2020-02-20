package worker;

import meta.*;
import static meta.Name.*;
import storage.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import java.util.regex.*;

public class ParameterMatchingWorker {

    private static final Pattern ANY_WITH_TYPE_MATCHER = Pattern.compile("any\\((.+)\\.class\\)");
    private static final Pattern EQUAL_MATCHER = Pattern.compile("eq\\((.+)\\)");
    private static final Pattern SAME_MATCHER = Pattern.compile("same\\((.+)\\)");

    private static final String[] MOCKITO_SIMPLE_MATCHER_SUFFIX = new String[]{
        "anyInt()",
        "anyLong()",
        "anyString()",
        "anyList()",
        "anyBoolean()",
        "any()",
        "anyMap()",
        "anyIterable()",
        "isNull()"
    };
    private static final String[] JMOCKIT_SIMPLE_MATCHERS = new String[]{
        "anyInt",
        "anyLong",
        "anyString",
        "(java.util.List)any",
        "anyBoolean",
        "null",
        "(java.util.Map)any",
        "(Iterable)any",
        "null"
    };

    private MethodWorker methodWorker;

    private ParameterMatchingWorker(MethodWorker mlw) {
        methodWorker = mlw;
    }

    public static ParameterMatchingWorker forWorker(MethodWorker mlw) {
        return new ParameterMatchingWorker(mlw);
    }

    public NodeList<Expression> leach(Craft craft) {
        String input = craft.getCallMeta().getInput();
        if (input.trim().isEmpty()) {
            return new NodeList<>();
        }
        String[] elements = input.split(",");
        int len = elements.length;
        NodeList<Expression> output = new NodeList<>();
        String[][] paramTypes = new String[0][0];
        for (int i = 0; i < len; ++i) {
            Expression arg = elementLeach(elements[i].trim());
            if (arg == null) {
                String subject = craft.getSubjectName();
                String[] subjectType = methodWorker.findType(subject);
                String method = craft.getMethodName();
                if (subjectType.length == 2) {
                    //paramTypes = CodeBaseStorage.findType(subjectType, method, len);
                    throw new RuntimeException("Under Construction");
                }
                if (paramTypes.length == 0 || paramTypes[i].length != 2) {
                    output.add(new NameExpr("null"));
                    WoodLog.attach(ERROR, "Cannot find concrete type of "
                            + (subjectType.length == 2 ? subjectType[0] : subject)
                            + "::" + method + "::" + len);
                } else {
                    output.add(new NameExpr("("+paramTypes[i][0]+")any"));
                    if ( ! paramTypes[i][1].isEmpty()) {
                        methodWorker.addImportationIfAbsent( paramTypes[i][1] );
                    }
                }
            } else {
                output.add(arg);
            }
        }
        return output;
    }

    private Expression elementLeach(String el) {
        Expression output = trySimpleTranslate(el);
        if (output != null) {
            return output;
        }
        // any(WithClass.class)
        Matcher m = ANY_WITH_TYPE_MATCHER.matcher(el);
        if (m.find()) {
            return new NameExpr("("+m.group(1)+")any");
        }
        // eq("something")
        m = EQUAL_MATCHER.matcher(el);
        if (m.find()) {
            return new NameExpr(m.group(1));
        }
        // same(something)
        m = SAME_MATCHER.matcher(el);
        if (m.find()) {
            return new NameExpr(m.group(1));
        }
        // and/not/or/aryEq/cmpEq more at https://site.mockito.org/javadoc/current/org/mockito/AdditionalMatchers.html
        return new NameExpr(el);
    }

    private Expression trySimpleTranslate(String el) {
        for (int i = 0; i < MOCKITO_SIMPLE_MATCHER_SUFFIX.length; ++i) {
            if (el.endsWith(MOCKITO_SIMPLE_MATCHER_SUFFIX[i])) {
                /*
                if (i == 3) { // reach anyList
                    methodWorker.addImportationIfAbsent("java.util.List");
                }
                */
                return new NameExpr(JMOCKIT_SIMPLE_MATCHERS[i]);
            }
        }
        return null;
    }
}
