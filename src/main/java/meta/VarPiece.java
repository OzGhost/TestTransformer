package meta;

public class VarPiece {

    private static final String D_NAME = "d98ef73f6fb9dfc43d58053fb21273a9";
    private static final String D_TYPE = "b244d4cdc075a812ea0cdd59efdc3685";
    private static final String D_SRC = "4c6001b499712acecf647605e835021e";

    private String name;
    private String type;
    private String src;

    public VarPiece() {
        name = D_NAME;
        type = D_TYPE;
        src = D_SRC;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public String getType() {
        return type;
    }

    public void setType(String t) {
        type = t;
    }

    public String getSource() {
        return src;
    }

    public void setSource(String s) {
        src = s;
    }

    public boolean sameTypeWith(VarPiece v) {
        return type.equals(v.type);
    }

    public boolean sameNameWith(VarPiece v) {
        return name.equals(v.name);
    }

    @Override
    public String toString() {
        return type + " : " + name + " : " + src;
    }
}

