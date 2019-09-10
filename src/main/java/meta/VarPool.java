package meta;

import static meta.Name.WARNING;

import worker.WoodLog;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;

public class VarPool implements Iterable<VarPiece> {
    
    private List<VarPiece> vars = new LinkedList<>();

    public VarHooker addVar(String varName) {
        return new VarHooker(this, varName);
    }

    public List<VarPiece> findAllByType(String type) {
        List<VarPiece> output = new LinkedList<>();
        for (VarPiece v: vars) {
            if (v.getType().equals(type))
                output.add(v);
        }
        return output;
    }

    public VarPiece find(String s) {
        for (VarPiece v: vars) {
            if (v.getName().equals(s) || v.getType().equals(s))
                return v;
        }
        return null;
    }

    public boolean nameInPool(String name) {
        return findByName(name) != null;
    }

    public VarPiece findByName(String name) {
        for (VarPiece v: vars) {
            if (v.getName().equals(name))
                return v;
        }
        return null;
    }

    public boolean typeInPool(String type) {
        return findByType(type) != null;
    }

    public VarPiece findByType(String type) {
        for (VarPiece v: vars) {
            if (v.getType().equals(type))
                return v;
        }
        return null;
    }

    @Override
    public Iterator<VarPiece> iterator() {
        return vars.iterator();
    }

    public void remove(VarPiece rmv) {
        List<VarPiece> nextVars = new LinkedList<>();
        for (VarPiece v: vars) {
            if (rmv != v) nextVars.add(v);
        }
        vars = nextVars;
    }

    public static class VarHooker {

        private VarPool master;
        private VarPiece piece = new VarPiece();

        private VarHooker(VarPool m, String vName) {
            master = m;
            piece.setName(vName);
        }

        public VarHooker underType(String vType) {
            piece.setType(vType);
            return this;
        }

        public void from(String src) {
            piece.setSource(src);
            master.vars.add(piece);
        }
    }
}

