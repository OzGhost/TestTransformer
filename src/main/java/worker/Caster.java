package worker;

import java.util.List;
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
        boolean fakingRequired = false;
        List<Craft> crafts = records.toCrafts();
        for (Craft c: crafts) {
            if (c.getCallMeta().isPrivate()) {
                fakingRequired = true;
                break;
            }
        }
        if (fakingRequired) {
            System.out.println("Simulator: faking invoked");
        }
        Statement mockingReplayed = MockWorker.forWorker(worker).transform(records, worker.getPMV());
        return mockingReplayed;
    }
}

