package worker;

import java.util.List;
import java.util.LinkedList;

import meta.Craft;
import meta.MockingMeta;
import com.github.javaparser.ast.stmt.Statement;

public class Caster {

    private MethodWorker worker;

    private Caster(MethodWorker mw) {
        worker = mw;
    }

    public static Caster forWorker(MethodWorker mw) {
        return new Caster(mw);
    }

    public Statement replay(MockingMeta records) {
        List<Craft> mockingCrafts = new LinkedList<>();
        List<Craft> fakingCrafts = new LinkedList<>();
        for (Craft c: records.toCrafts()) {
            if (c.getCallMeta().isPrivate()) {
                fakingCrafts.add(c);
            } else {
                mockingCrafts.add(c);
            }
        }
        if ( ! fakingCrafts.isEmpty()) {
            System.out.println("Faking required!");
        }
        Statement mockingReplayed = MockWorker.forWorker(worker).transform(mockingCrafts, worker.getPMV());
        return mockingReplayed;
    }
}

