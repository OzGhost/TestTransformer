package storage;

public class ParameterPack {

    private String[][] pack;

    public boolean isEmpty() {
        return pack == null;
    }

    public String[][] getPack() {
        return pack;
    }

    public void setPack(String[][] p) {
        pack = p;
    }
}

