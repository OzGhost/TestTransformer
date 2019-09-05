package meta;

import meta.Craft;
import java.util.List;
import java.util.LinkedList;

public class StatementPiece {
    private int type;
    private Craft craft;
    private String requestAsMock;

    public StatementPiece(int t) {
        type = t;
    }

    public int getType() {
        return type;
    }

    public String getRequestAsMock() {
        return requestAsMock;
    }

    public StatementPiece requestMock(String type) {
        requestAsMock = type;
        return this;
    }

    public Craft getCraft() {
        return craft;
    }

    public StatementPiece beWith(Craft c) {
        craft = c;
        return this;
    }
}

