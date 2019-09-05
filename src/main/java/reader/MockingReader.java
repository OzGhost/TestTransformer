package reader;

import meta.*;
import com.github.javaparser.ast.*;

public abstract class MockingReader {

    protected static final String STATIC_RECALL_PATTERN_SUFFIX = "\\.([^\\(]+)\\((.*)\\)";

    abstract public StatementPiece read(String stm, Node node, Node belowNode);
}
