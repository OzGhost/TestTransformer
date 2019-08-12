package storage;

public class CodeBaseStorage {

    private static final PackageDAS packageDas = new PackageDAS();

    public static String[][] findType(String[] type, String method, int parameterCount) {
        MethodDAS methodDas = packageDas.findByPackage(type[1]).findByType(type[0]);
        if (methodDas.isEmpty()) {
            System.out.println("Simulation: load " + type[1]+"."+type[0] + ":" + method + ":" + parameterCount);
        }
        ParameterPack paramPack = methodDas.findByMethod(method).findByParameterCount(parameterCount);
        return paramPack.getPack();
    }
}

