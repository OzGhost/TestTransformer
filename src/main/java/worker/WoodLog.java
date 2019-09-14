package worker;

import meta.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.FileWriter;

public class WoodLog {

    private static ThreadLocal<Cut> cCut = new ThreadLocal<>();
    private static Queue<Cut> cuts = new ConcurrentLinkedQueue<>();
    private static FileWriter writer;

    static {
        try {
            writer = new FileWriter("./wood.log");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void reachClass(String className) {
        Cut c = new Cut();
        c.classLevel = className;
        cCut.set(c);
    }

    public static void reachMethod(String methodName) {
        cCut.get().methodLevel = methodName;
    }

    public static void reachSubject(String subjectName) {
        cCut.get().subjectLevel = subjectName;
    }

    public static Cut attach(int level, String msg) {
        return attach(level, cCut.get().subjectLevel, "", CallMeta.NIL, msg);
    }

    public static Cut attach(int level, String subject, String message) {
        return attach(level, subject, "", CallMeta.NIL, message);
    }

    public static Cut attach(int level, CallMeta callMeta, String message) {
        return attach(level, cCut.get().subjectLevel, "", callMeta, message);
    }

    public static Cut attach(int level, String call, CallMeta callMeta, String message) {
        return attach(level, cCut.get().subjectLevel, call, callMeta, message);
    }

    public static Cut attach(int level, String subject, String call, CallMeta callMeta, String message) {
        Cut cut = new Cut();
        Cut c = cCut.get();
        cut.level = level;
        cut.classLevel = c.classLevel;
        cut.methodLevel = c.methodLevel;
        cut.subjectLevel = subject;
        cut.call = call;
        cut.callMeta = callMeta;
        cut.message = message;
        synchronized(WoodLog.class) {
            try {
                writer.write(cut.toString());
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        //cuts.offer(cut);
        return cut;
    }

    public static Queue<Cut> getCuts() {
        return cuts;
    }

    public static void printCuts() {
        if (cuts.isEmpty()) {
            System.out.println("((() There are no cut");
            return;
        }
        for (Cut c: cuts) {
            System.out.println(c);
        }
    }

    public static class Cut {
        private String classLevel;
        private String methodLevel;
        private String subjectLevel;
        private String call;
        private CallMeta callMeta;
        private String message;
        private int level;

        public String getClassLevel() {
            return classLevel;
        }

        public String getMethodLevel() {
            return methodLevel;
        }

        public String getSubjectLevel() {
            return subjectLevel;
        }

        public String getCall() {
            return call;
        }

        public CallMeta getCallMeta() {
            return callMeta;
        }

        public String getMessage() {
            return message;
        }

        public int getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                .append("[")
                .append(level)
                .append("]WL ... ->> : ")
                .append(message)
                .append("\n---- ---- ---- ")
                .append(classLevel)
                .append(" :: ")
                .append(methodLevel)
                .append(" :: ")
                .append(subjectLevel)
                .append(" :: ")
                .append(call)
                .append(" :: ")
                .append(callMeta.toString())
                .toString();
        }
    }
}
