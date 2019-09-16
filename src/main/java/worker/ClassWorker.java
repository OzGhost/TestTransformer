package worker;

import static meta.Name.ERROR;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
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
    private Map<String, Set<String>> hijackedTypes;

    public ClassWorker setCompilationUnitWorker(CompilationUnitWorker cunit) {
        cUnitWorker = cunit;
        return this;
    }

    public void transform(ClassOrInterfaceDeclaration classUnit) {
        WoodLog.reachClass(classUnit.getName().asString());

        List<FieldDeclaration> fields = new LinkedList<>();
        List<MethodDeclaration> methods = new LinkedList<>();

        for (BodyDeclaration<?> declaration: classUnit.getMembers()) {
            if (declaration instanceof FieldDeclaration) {
                fields.add( (FieldDeclaration) declaration);
            } else if (declaration instanceof MethodDeclaration){
                methods.add( (MethodDeclaration) declaration);
            }
        }

        this.hijackedTypes = cookTheHijackedFields(fields);
        Set<String> ics = removeIC(fields);
        Set<String> takenNames = collectTakenNames(classUnit);

        eliminatePrepareBlock(methods);
        //CallGraph callGraph = ClassScanner.scanCallGraph(methods);

        /*
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
        */

        for (MethodDeclaration methodUnit: methods) {
            new MethodWorker(methodUnit)
                .setClassWorker(this)
                //.setRequiredFields(mockedFields)
                .setTakenNames(takenNames)
                .setICs(ics)
                .transform();
        }

        //reConnect(callGraph);
        //cleanUp(methods);
        reHijack(classUnit);
    }

    private Set<String> collectTakenNames(ClassOrInterfaceDeclaration classUnit) {
        Set<String> names = new HashSet<>();
        for (NameExpr n: classUnit.findAll(NameExpr.class)) {
            names.add(n.getName().asString());
        }
        return names;
    }

    private Map<String, Set<String>> cookTheHijackedFields(List<FieldDeclaration> fields) {
        Map<String, Set<String>> output = new HashMap<>();
        List<Node> useless = new LinkedList<>();
        for (FieldDeclaration f: fields) {
            AnnotationExpr a = getHijackAnnotation(f);
            if (a == null) continue;
            //a.replace(new MarkerAnnotationExpr("Mocked"));

            String type = "";
            Set<String> names = new HashSet<>();
            for (VariableDeclarator v: f.getVariables()) {
                type = v.getType().asString();
                names.add(v.getName().asString());
            }
            output.put(type, names);
            useless.add(f);
        }
        for (Node n: useless) n.remove();
        return output;
    }

    private AnnotationExpr getHijackAnnotation(FieldDeclaration f) {
        for (AnnotationExpr a: f.getAnnotations()) {
            String aName = a.getName().asString();
            if ("Mock".equals(aName) || "InjectMocks".equals(aName)) {
                return a;
            }
        }
        return null;
    }

    private Set<String> removeIC(List<FieldDeclaration> fields) {
        Set<String> icNames = new HashSet<>();
        List<Node> useless = new LinkedList<>();
        for (FieldDeclaration f: fields) {
            String ftype = f.getVariables().get(0).getType().asString();
            if ("InvocationCounter".equals(ftype) || "ch.axonivy.fintech.standard.core.mock.InvocationCounter".equals(ftype)) {
                for (VariableDeclarator v: f.getVariables()) {
                    icNames.add(v.getName().asString());
                }
                useless.add(f);
            }
        }
        for (Node n: useless) n.remove();
        return icNames;
    }


    private void eliminatePrepareBlock(List<MethodDeclaration> methods) {
        List<Entry<String, List<ReferenceType>>> preFuncs = new LinkedList<>();
        List<MethodDeclaration> testBlocks = new LinkedList<>();
        List<Node> useless = new LinkedList<>();
        for (MethodDeclaration mUnit: methods) {
            for (AnnotationExpr annotation: mUnit.getAnnotations()) {
                String annotationName = annotation.getName().asString();
                if ("Before".equals(annotationName)) {
                    useless.add(annotation);
                    String pFunName = mUnit.getName().asString();
                    List<ReferenceType> exs = extractExs(mUnit);
                    preFuncs.add(new SimpleEntry<>(pFunName, exs));
                    break;
                } else if ("Test".equals(annotationName)){
                    testBlocks.add(mUnit);
                    break;
                }
            }
        }
        for (Entry<String, List<ReferenceType>> preFunc: preFuncs) {
            for (MethodDeclaration testBlock: testBlocks) {
                recallPrepareFuncInTestBlock(testBlock, preFunc);
            }
        }
        for (Node u: useless) {
            u.remove();
        }
    }

    private List<ReferenceType> extractExs(MethodDeclaration mUnit) {
        List<ReferenceType> exs = new LinkedList<>();
        for (ReferenceType e: mUnit.getThrownExceptions()) {
            exs.add(e);
        }
        return exs;
    }

    private void recallPrepareFuncInTestBlock(MethodDeclaration testBlocks, Entry<String, List<ReferenceType>> preFunc) {
        NodeList<Statement> nextStms = new NodeList<>();
        nextStms.add( new ExpressionStmt(new MethodCallExpr(preFunc.getKey())) );
        NodeList<Statement> currentStms = testBlocks.getBody().get().getStatements();
        nextStms.addAll(currentStms);
        testBlocks.getBody().get().setStatements(nextStms);
        Set<String> thrown = new HashSet<>();
        for (ReferenceType e: extractExs(testBlocks)) {
            thrown.add(e.asString());
        }
        for (ReferenceType e: preFunc.getValue()) {
            if ( ! thrown.contains(e.asString())) {
                testBlocks.getThrownExceptions().add(e);
            }
        }
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

    public String recordAsInstanceMocked(String type, String suggestName, Set<String> usedNames) {
        Set<String> nameOfType = hijackedTypes.get(type);
        if (nameOfType == null) {
            hijackedTypes.put(type, asSet(suggestName));
            return suggestName;
        }
        for (String name: nameOfType) {
            if ( ! usedNames.contains(type+":"+name)) {
                return name;
            }
        }
        if ( ! nameOfType.add(suggestName)) {
            WoodLog.attach(ERROR, "Duplicate mocked name");
        }
        return suggestName;
    }

    private Set<String> asSet(String e) {
        Set<String> set = new HashSet<>();
        set.add(e);
        return set;
    }

    public boolean recordAsTypeMocked(String type, String suggestName) {
        if ( ! hijackedTypes.containsKey(type)) {
            hijackedTypes.put(type, asSet(suggestName));
            return true;
        }
        return false;
    }

    private void reHijack(ClassOrInterfaceDeclaration classUnit) {
        NodeList<BodyDeclaration<?>> newBody = new NodeList<>();
        for (Entry<String, Set<String>> hijacked: hijackedTypes.entrySet()) {
            String type = hijacked.getKey();
            NodeList<VariableDeclarator> vars = new NodeList<>();
            for (String name: hijacked.getValue()) {
                vars.add(
                        new VariableDeclarator(
                            new ClassOrInterfaceType(null, type),
                            name
                        )
                    );
            }
            FieldDeclaration f = new FieldDeclaration();
            f.setVariables(vars);
            f.setAnnotations(new NodeList<>( new MarkerAnnotationExpr("Mocked") ));
            newBody.add(f);
        }
        newBody.addAll(classUnit.getMembers());
        classUnit.setMembers(newBody);
    }
}

