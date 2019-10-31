package worker;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map.Entry;

import meta.Craft;
import meta.MockingMeta;
import meta.SubjectMeta;
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
        if ( fakingSubjects.isEmpty() ) {
            Statement mockingReplayed = MockWorker.forWorker(worker).transform(records.toCrafts());
            return new Statement[]{mockingReplayed};
        }

        Set<String> fakingTypes = new HashSet<>();
        for (String subject: fakingSubjects) {
            String type = null;
            if ( ! isAType(subject)) {
                type = worker.findTypeWithoutPackage(subject);
            } else {
                type = subject;
            }
            if (type == null) {
                WoodLog.attach("Due to type not found -> Ignore subject [" +subject+ "] !");
                continue;
            }
            fakingTypes.add(type);
        }
        MockingMeta mockMetas = new MockingMeta();
        MockingMeta fakeMetas = new MockingMeta();
        for (Entry<String, SubjectMeta> subjectEntry: records.getSubjectMetas().entrySet()) {
            boolean isFakingSubject = false;
            String subjectName = subjectEntry.getKey();
            if (fakingTypes.contains(subjectName)) {
                isFakingSubject = true;
                fakeMetas.mergeSubjectMeta(subjectName, subjectEntry.getValue());
            } else {
                if ( ! isAType(subjectName) && ! isChainedCallSubject(subjectName) ) {
                    String subjectType = worker.findTypeWithoutPackage(subjectName);
                    if (fakingTypes.contains(subjectType)) {
                        isFakingSubject = true;
                        fakeMetas.mergeSubjectMeta(subjectType, subjectEntry.getValue());
                    }
                }
            }
            if ( ! isFakingSubject) {
                mockMetas.loadEntry(subjectEntry);
            }
        }
        List<Statement> expectations = FakeWorker.transform(fakeMetas);
        /*
        System.out.println("Mock metas");
        System.out.println(mockMetas);
        System.out.println("Fake metas");
        System.out.println(fakeMetas);
        */

        Statement mockReplay = MockWorker.forWorker(worker).transform(mockMetas.toCrafts());
        expectations.add( mockReplay );
        return expectations.toArray(new Statement[expectations.size()]);
    }
    
    private boolean isAType(String type) {
        char firstChar = type.charAt(0);
        int lastDotIndex = type.lastIndexOf('.');
        if (lastDotIndex > 0) {
            firstChar = type.charAt(lastDotIndex+1);
        }
        if ('A' <= firstChar && firstChar <= 'Z') {
            return true;
        }
        return false;
    }

    private boolean isChainedCallSubject(String subject) {
        return subject.contains(".") && subject.contains(")");
    }
}

