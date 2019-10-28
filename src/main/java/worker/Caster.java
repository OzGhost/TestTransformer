package worker;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

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

    public Statement[] replay(MockingMeta records) {
        Set<String> fakingSubjects = new HashSet<>();
        for (Craft c: records.toCrafts()) {
            if (c.getCallMeta().isPrivate()) {
                fakingSubjects.add(c.getSubjectName());
            }
        }
        if ( ! fakingSubjects.isEmpty() ) {
            System.out.println("--=--");
            for (String subject: fakingSubjects) {
                String type = null;
                char firstChar = subject.charAt(0);
                if (firstChar < 'A' || 'Z' < firstChar) {
                    type = worker.findTypeWithoutPackage(subject);
                } else {
                    type = subject;
                }
                
                if (type == null) {
                    //WoodLog.attach("Type of ["+subject+"] not found");
                }
                if ( ! subject.equals(type)) {
                    System.out.println(subject + " : " + type);
                }
            }
        }
        Statement mockingReplayed = MockWorker.forWorker(worker).transform(records.toCrafts(), worker.getPMV());
        return new Statement[]{mockingReplayed};
    }
}

