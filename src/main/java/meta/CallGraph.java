package meta;

import java.util.Map;
import java.util.List;

public class CallGraph {
    private Map<String, CallDash> graph;
    private List<String> roots;

    public CallGraph(Map<String, CallDash> g, List<String> r) {
        graph = g;
        roots = r;
    }

    public Map<String, CallDash> getGraph() {
        return graph;
    }

    public List<String> getRoots() {
        return roots;
    }
}

