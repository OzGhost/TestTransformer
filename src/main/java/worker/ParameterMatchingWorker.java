package worker;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import java.util.regex.*;

public class ParameterMatchingWorker {

    private static final Pattern ANY_WITH_TYPE_MATCHER = Pattern.compile("any\\((.+)\\.class\\)");
    private static final Pattern EQUAL_MATCHER = Pattern.compile("eq\\((.+)\\)");

    private static final String[] MOCKITO_SIMPLE_MATCHER_SUFFIX = new String[]{
        "anyInt()",
        "anyLong()",
        "anyString()",
        "anyList()",
        "anyBoolean()"
    };
    private static final String[] JMOCKIT_SIMPLE_MATCHERS = new String[]{
        "anyInt",
        "anyLong",
        "anyString",
        "(List)any",
        "anyBoolean"
    };

    private static ClassWorker classLevelWorker = new ClassWorker();

    public static void registerClassLevelWorker(ClassWorker cw) {
        classLevelWorker = cw;
    }

    public static NodeList<Expression> leach(String input) {
        System.out.println("found: '" + input + "'");
        if (input.trim().isEmpty()) {
            return new NodeList<>();
        }
        String[] elements = input.split(",");
        NodeList<Expression> output = new NodeList<>();
        for (int i = 0; i < elements.length; ++i) {
            Expression arg = elementLeach(elements[i].trim());
            if (arg == null) {
                // need special look up for correct type :D
                output.add(new NameExpr("NoYou"));
            } else {
                output.add(arg);
            }
        }
        return output;
    }

    private static Expression elementLeach(String el) {
        if (el.contains("any()")) {
            return null;
        }
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
        // and/not/or/aryEq/cmpEq more at https://site.mockito.org/javadoc/current/org/mockito/AdditionalMatchers.html
        return new NameExpr(el);
    }

    private static Expression trySimpleTranslate(String el) {
        for (int i = 0; i < MOCKITO_SIMPLE_MATCHER_SUFFIX.length; ++i) {
            if (el.endsWith(MOCKITO_SIMPLE_MATCHER_SUFFIX[i])) {
                return new NameExpr(JMOCKIT_SIMPLE_MATCHERS[i]);
            }
        }
        return null;
    }
}
