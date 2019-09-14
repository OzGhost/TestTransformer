package worker;

import java.util.*;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import meta.CallGraph;
import meta.CallDash;

public class ClassWorker {

    private CompilationUnitWorker cUnitWorker;

    public ClassWorker setCompilationUnitWorker(CompilationUnitWorker cunit) {
        cUnitWorker = cunit;
        return this;
    }

    public void transform(ClassOrInterfaceDeclaration classUnit) {
        WoodLog.reachClass(classUnit.getName().asString());

        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();

        for (BodyDeclaration<?> declaration: classUnit.getMembers()) {
            if (declaration instanceof FieldDeclaration) {
                fields.add( (FieldDeclaration) declaration);
            } else if (declaration instanceof MethodDeclaration){
                methods.add( (MethodDeclaration) declaration);
            }
        }

        eliminatePrepareBlock(methods);
        CallGraph callGraph = ClassScanner.scanCallGraph(methods);

        List<VariableDeclarator> mockedFields = new ArrayList<>();
        List<String> icNames = new ArrayList<>();
        for (FieldDeclaration fieldUnit: fields) {
            if ( isMockField(fieldUnit) ) {
                mockedFields.addAll(fieldUnit.getVariables());
                fieldUnit.remove();
            } else if ( isIC(fieldUnit) ) {
                for (VariableDeclarator v: fieldUnit.getVariables()) {
                    icNames.add( v.getName().asString() );
                }
                fieldUnit.remove();
            }
        }

        for (MethodDeclaration methodUnit: methods) {
            new MethodWorker(methodUnit)
                .setClassWorker(this)
                .setRequiredFields(mockedFields)
                .setICNames(icNames)
                .transform();
        }

        reConnect(callGraph);
        cleanUp(methods);
    }

    private void eliminatePrepareBlock(List<MethodDeclaration> methods) {
        List<String> prepareFuncNames = new LinkedList<>();
        List<MethodDeclaration> testBlocks = new LinkedList<>();
        List<Node> useless = new LinkedList<>();
        for (MethodDeclaration mUnit: methods) {
            for (AnnotationExpr annotation: mUnit.getAnnotations()) {
                String annotationName = annotation.getName().asString();
                if ("Before".equals(annotationName)) {
                    useless.add(annotation);
                    prepareFuncNames.add(mUnit.getName().asString());
                } else if ("Test".equals(annotationName)){
                    testBlocks.add(mUnit);
                }
            }
        }
        for (String prepareFuncName: prepareFuncNames) {
            for (MethodDeclaration testBlock: testBlocks) {
                recallPrepareFuncInTestBlock(testBlock, prepareFuncName);
            }
        }
        for (Node u: useless) {
            u.remove();
        }
    }

    private void recallPrepareFuncInTestBlock(MethodDeclaration testBlocks, String prepareFuncName) {
        NodeList<Statement> nextStms = new NodeList<>();
        nextStms.add( new ExpressionStmt(new MethodCallExpr(prepareFuncName)) );
        NodeList<Statement> currentStms = testBlocks.getBody().get().getStatements();
        nextStms.addAll(currentStms);
        testBlocks.getBody().get().setStatements(nextStms);
    }

    private boolean isMockField(FieldDeclaration f) {
        for (AnnotationExpr fa: f.getAnnotations()) {
            if ( "Mock".equals(fa.getName().asString()) ) {
                return true;
            }
        }
        return false;
    }

    private boolean isIC(FieldDeclaration f) {
        Type t = f.getVariables().get(0).getType();
        return "InvocationCounter".equals(t.asString());
    }

    public void addImportationIfAbsent(String im) {
        cUnitWorker.addImportationIfAbsent(im);
    }

    public String[] findType(String type) {
        return cUnitWorker.findType(type);
    }

    private void reConnect(CallGraph graph) {
        Set<Integer> connected = new HashSet<>();
        for (Deque<CallDash> callStack: toStacks(graph)) {
            while(!callStack.isEmpty()) {
                CallDash dash = callStack.pop();
                Integer dashCode = System.identityHashCode(dash);
                if ( ! connected.contains(dashCode)){
                    reConnect(dash);
                    connected.add(dashCode);
                }
            }
        }
    }

    private List<Deque<CallDash>> toStacks(CallGraph graph) {
        Set<String> stacked = new HashSet<>();
        List<Deque<CallDash>> stacks = new LinkedList<>();
        Map<String, CallDash> rawGraph = graph.getGraph();
        for (String rCode: graph.getRoots()) {
            Deque<CallDash> stackOfCurrentRoot = new LinkedList<>();
            Queue<String> tails = new LinkedList<>();
            tails.offer(rCode);
            while (!tails.isEmpty()) {
                String sig = tails.poll();
                if (stacked.contains(sig)) {
                    continue;
                }
                CallDash c = rawGraph.get(sig);
                stackOfCurrentRoot.push(c);
                for (String callSig: c.getCalleesSignatures()) {
                    tails.offer(callSig);
                }
                stacked.add(sig);
            }
            stacks.add( stackOfCurrentRoot );
        }
        return stacks;
    }

    private void reConnect(CallDash dash) {
        if (dash.getCallees().length == 0) {
            return;
        }
        MethodDeclaration caller = dash.getCaller();
        Map<String, List<String>> typeLeadMap = getMockedTypeLeadMap(caller);
        List<String[]> requested = new LinkedList<>();
        Set<String> takenNames = null;
        int len = dash.getCallees().length;
        for (int i = 0; i < len; ++i) {
            MethodDeclaration callee = dash.getCallees()[i];
            List<Parameter> mParams = getMockedParameters(callee);
            if (mParams.isEmpty()) {
                continue;
            }
            Set<String> used = new HashSet<>();
            String[] newArgs = new String[mParams.size()];
            int j = 0;
            for (Parameter p: mParams) {
                String type = p.getType().asString();
                List<String> namesUnderType = typeLeadMap.get(type);
                if (namesUnderType == null) {
                    if (takenNames == null) {
                        takenNames = loadTakenName(caller);
                    }
                    String newName = NameUtil.createTypeBasedName(type, takenNames);
                    takenNames.add(newName);
                    newArgs[j] = newName;
                    requested.add(new String[]{type, newName});
                    typeLeadMap.put(type, asList(newName));
                    used.add(type+":"+newName);
                } else {
                    String usableName = "";
                    for (String nut: namesUnderType) {
                        if (!used.contains(type+":"+nut)) {
                            usableName = nut;
                            break;
                        }
                    }
                    if (usableName.isEmpty()) {
                        usableName = NameUtil.createTypeBasedName(type, takenNames);
                        takenNames.add(usableName);
                        requested.add(new String[]{type, usableName});
                        namesUnderType.add(usableName);
                    }
                    newArgs[j] = usableName;
                    used.add(type+":"+usableName);
                }
                j++;
            }
            appendNewArgs(dash.getConnectors()[i], newArgs);
        }
        appendNewMocked(caller, requested);
    }

    private Map<String, List<String>> getMockedTypeLeadMap(MethodDeclaration call) {
        Map<String, List<String>> typeLeadMap = new HashMap<>();
        for (Parameter p: getMockedParameters(call)) {
            String type = p.getType().asString();
            String name = p.getName().asString();
            List<String> namesUnderType = typeLeadMap.get(type);
            if (namesUnderType == null) {
                namesUnderType = new LinkedList<>();
                typeLeadMap.put(type, namesUnderType);
            }
            namesUnderType.add(name);
        }
        return typeLeadMap;
    }

    private List<Parameter> getMockedParameters(MethodDeclaration call) {
        List<Parameter> output = new LinkedList<>();
        for (Parameter p: call.getParameters()) {
            for (AnnotationExpr a: p.getAnnotations()) {
                if ("Mocked".equals(a.getName().asString())) {
                    output.add(p);
                }
            }
        }
        return output;
    }

    private Set<String> loadTakenName(MethodDeclaration call) {
        Set<String> output = new HashSet<>();
        for (Name n: call.findAll(Name.class)) {
            output.add(n.asString());
        }
        return output;
    }

    private List<String> asList(String a) {
        List<String> l = new LinkedList<>();
        l.add(a);
        return l;
    }

    private void appendNewArgs(MethodCallExpr call, String[] newArgs) {
        for (String arg: newArgs) {
            call.getArguments().add(new NameExpr(arg));
        }
    }

    private void appendNewMocked(MethodDeclaration mUnit, List<String[]> requested) {
        for (String[] r: requested) {
            Parameter p = new Parameter(
                    new ClassOrInterfaceType(null, r[0]),
                    r[1]
                );
            p.setAnnotations(new NodeList<>(new MarkerAnnotationExpr("Mocked")));
            mUnit.getParameters().add(p);
        }
    }

    private void cleanUp(List<MethodDeclaration> methods) {
        List<Node> useless = new LinkedList<>();
        for (MethodDeclaration m: methods) {
            if (isTestMethod(m)) {
                continue;
            }
            for (Parameter p: m.getParameters()) {
                for (AnnotationExpr a: p.getAnnotations()) {
                    if ("Mocked".equals(a.getName().asString())) {
                        useless.add(a);
                    }
                }
            }
        }
        for (Node u: useless) {
            u.remove();
        }
    }

    private boolean isTestMethod(MethodDeclaration m) {
        for (AnnotationExpr a: m.getAnnotations()) {
            if ("Test".equals(a.getName().asString())) {
                return true;
            }
        }
        return false;
    }
}

