package worker;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class NameUtil {

    private static final char CASE_DELTA = 'a' - 'A';

    public static final String createTypeBasedName(String varType, Set<String> usedVarName) {
        char firstCharLowerCase = varType.charAt(0);
        if ('A' <= firstCharLowerCase && firstCharLowerCase <= 'Z') {
            firstCharLowerCase = (char)(varType.charAt(0) + CASE_DELTA);
        }
        String base = new StringBuilder(varType.length() + 1)
                            .append('_')
                            .append(firstCharLowerCase)
                            .append(varType.substring(1))
                            .toString()
                            .replaceAll("[<>]", "_") // eliminate generic type
                            .replaceAll("\\.", "_"); // eliminate dot in package included type
        String output = base;
        int version = 1;
        while (usedVarName.contains(output)) {
            WoodLog.loopLog(NameUtil.class, 27);
            version++;
            output = base + "_v" + version;
        }
        return output;
    }

    public static final Map<String, String> decompileImports(List<String> ims) {
        Map<String, String> out = new HashMap<>();
        for (String im: ims) {
            String[] di = decompileImportation(im);
            out.put(di[0], di[1]);
        }
        return out;
    };

    private static String[] decompileImportation(String im) {
        char[] imChars = im.toCharArray();
        int len = imChars.length;
        int i = len - 1;
        char[] typeStack = new char[len];
        int j = 0;
        for (; i >= 0 && imChars[i] != '.'; --i) {
            typeStack[j++] = imChars[i];
        }
        int k = 0;
        char[] type = new char[j];
        while (j > 0) {
            WoodLog.loopLog(NameUtil.class, 55);
            type[k++] = typeStack[--j];
        }
        char[] pkg = new char[i];
        k = 0;
        for (; k < i; ++k) {
            pkg[k] = imChars[k];
        }
        return new String[]{new String(type), new String(pkg)};
    }
}
