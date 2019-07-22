package reader;

import meta.*;
import com.github.javaparser.ast.*;

public abstract class MockingReader {

    protected static final String STATIC_RECALL_PATTERN_SUFFIX = "\\.([^\\(]+)\\((.*)\\)";

    protected Craft craft = new Craft();

    public Craft getCraft() {
        return craft;
    }

    abstract public int read(String stm, Node node, Node belowNode);
}
