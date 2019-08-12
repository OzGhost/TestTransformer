package storage;

public class CodeBaseStorage {

    private static final PackageDAS packageDas = new PackageDAS();

    public static String[][] findType(String[] type, String method, int parameterCount) {
        ParameterPack paramPack = packageDas.findByPackage(type[1])
                                                    .findByType(type[0])
                                                    .findByMethod(method)
                                                    .findByParameterCount(parameterCount);
        if (paramPack.isEmpty()) {
            System.out.println("Simulation: load " + type[1]+"."+type[0] + ":" + method + ":" + parameterCount);
        }
        return paramPack.getPack();
        return new String[0][0];
    }
}

