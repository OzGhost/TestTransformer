package worker;

import meta.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WoodLog {

    private static String currentClass = "";
    private static String currentMethod = "";
    private static String currentSubject = "";
    private static Queue<Cut> cuts = new ConcurrentLinkedQueue<>();

    public static void reachClass(String className) {
        cleanCurrentStamps();
        currentClass = className;
    }

    public static void reachMethod(String methodName) {
        currentMethod = methodName;
    }

    public static void reachSubject(String subjectName) {
        currentSubject = subjectName;
    }

    public static Cut attach(int level, String msg) {
        return attach(level, currentSubject, "", CallMeta.NIL, msg);
    }

    public static Cut attach(int level, String subject, String message) {
        return attach(level, subject, "", CallMeta.NIL, message);
    }

    public static Cut attach(int level, CallMeta callMeta, String message) {
        return attach(level, currentSubject, "", callMeta, message);
    }

    public static Cut attach(int level, String call, CallMeta callMeta, String message) {
        return attach(level, currentSubject, call, callMeta, message);
    }

    public static Cut attach(int level, String subject, String call, CallMeta callMeta, String message) {
        Cut cut = new Cut();
        cut.level = level;
        cut.classLevel = currentClass;
        cut.methodLevel = currentMethod;
        cut.subjectLevel = subject;
        cut.call = call;
        cut.callMeta = callMeta;
        cut.message = message;
        cuts.offer(cut);
        return cut;
    }

    private static void cleanCurrentStamps() {
        currentClass = "";
        currentMethod = "";
        currentSubject = "";
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
