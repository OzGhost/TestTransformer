package worker;

import java.util.Set;

public class NameUtil {

    private static final int CASE_DELTA = (int)'a' - (int)'A';

    public static final String createTypeBasedName(String varType, Set<String> usedVarName) {
        char firstCharLowerCase = (char)( (int) varType.charAt(0) + CASE_DELTA );
        String base = new StringBuilder(varType.length())
                            .append(firstCharLowerCase)
                            .append(varType.substring(1, varType.length()))
                            .toString();
        String output = base;
        int version = 1;
        while (usedVarName.contains(output)) {
            version++;
            output = base + "_v" + version;
        }
        return output;
    }
}
