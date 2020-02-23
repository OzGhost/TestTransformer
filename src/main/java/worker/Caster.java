package worker;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.HashMap;

import reader.ReaderUtil;
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
        List<Craft> crafts = records.toCrafts();
        for (Craft c: crafts) {
            if (c.getCallMeta().isPrivate()) {
                fakingSubjects.add(c.getSubjectName());
            }
        }
        if ( fakingSubjects.isEmpty() ) {
            Statement mockingReplayed = MockWorker.forWorker(worker).transform(crafts);
            return new Statement[]{mockingReplayed};
        }

        Map<String, String> fakingTypes = new HashMap<>();
        for (String subject: fakingSubjects) {
            String[] type = ReaderUtil.depart(subject);
            if (type[1] == null)
                type = worker.findType(subject);
            if (type == null) {
                WoodLog.attach("Due to type not found -> Ignore subject [" +subject+ "] !");
                continue;
            }
            if (fakingTypes.put(type[0],type[1]) != null){
                WoodLog.attach("Encounter same name type: " + type[0]);
            }
        }
        MockingMeta mockMetas = new MockingMeta();
        MockingMeta fakeMetas = new MockingMeta();
        for (Entry<String, SubjectMeta> subjectEntry: records.getSubjectMetas().entrySet()) {
            boolean isFakingSubject = false;
            String subjectName = subjectEntry.getKey();
            String pkg = fakingTypes.get( ReaderUtil.depart(subjectName)[0] );
            if (pkg != null) {
                isFakingSubject = true;
                fakeMetas.mergeSubjectMeta(subjectName, pkg, subjectEntry.getValue());
            } else {
                if ( ! ReaderUtil.isAType(subjectName) && ! isChainedCallSubject(subjectName) ) {
                    String subjectType = worker.findTypeWithoutPackage(subjectName);
                    pkg = fakingTypes.get(subjectType);
                    if (pkg != null) {
                        isFakingSubject = true;
                        fakeMetas.mergeSubjectMeta(subjectType, pkg, subjectEntry.getValue());
                    }
                }
            }
            if ( ! isFakingSubject) {
                mockMetas.loadEntry(subjectEntry);
            }
        }
        List<Statement> expectations = FakeWorker.transform(fakeMetas);
        List<Craft> mockCrafts = mockMetas.toCrafts();
        if ( ! mockCrafts.isEmpty()) {
            Statement mockReplay = MockWorker.forWorker(worker).transform(mockCrafts);
            expectations.add( mockReplay );
        }
        return expectations.toArray(new Statement[expectations.size()]);
    }
    
    private boolean isChainedCallSubject(String subject) {
        return subject.contains(".") && subject.contains(")");
    }
}

