package worker;

import static meta.Name.ERROR;
import static meta.Name.WARNING;

import storage.LibraryImplLoader;
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

    private static final String WHITEBOX_REP= "ch.axonivy.fintech.standard.core.mock.Whitebox";
    private static final String RESULT_VAR_REPLACEMENT_NAME = "resultOfExecution";
    private static final Node RESULT_VAR_REPLACEMENT_NODE = new NameExpr(RESULT_VAR_REPLACEMENT_NAME);

    private CompilationUnitWorker cUnitWorker;
    private Map<String, Set<String>> hijackedTypes;
    private Map<String, String> spiedVars;
    private Map<String, String> vars = new HashMap<>();

    public ClassWorker setCompilationUnitWorker(CompilationUnitWorker cunit) {
        cUnitWorker = cunit;
        return this;
    }

    public void transform(ClassOrInterfaceDeclaration rawClass) {

        ClassOrInterfaceDeclaration classUnit = rawClass;

        String className = classUnit.getName().asString();
        WoodLog.reachClass(className);

        List<FieldDeclaration> fields = new LinkedList<>();
        List<MethodDeclaration> methods = new LinkedList<>();
        
        loadLibraryImpl(classUnit);

        for (BodyDeclaration<?> declaration: classUnit.getMembers()) {
            if (declaration instanceof FieldDeclaration) {
                FieldDeclaration f = (FieldDeclaration) declaration;
                for (VariableDeclarator vari: f.getVariables()) {
                    vars.put(vari.getName().asString(), vari.getType().asString());
                }
                fields.add(f);
            } else if (declaration instanceof MethodDeclaration){
                methods.add( (MethodDeclaration) declaration);
            }
        }

        this.spiedVars = removeSpiedVariables(fields);
        this.hijackedTypes = cookTheHijackedFields(fields);
        Set<String> ics = removeIC(fields);
        Set<String> takenNames = collectTakenNames(classUnit);

        eliminatePrepareBlock(methods);
        classUnit = new Normalizer().normalize(classUnit);

        for (MethodDeclaration methodUnit: classUnit.findAll(MethodDeclaration.class)) {
            //if (true)break;
            new MethodWorker(methodUnit)
                .setClassWorker(this)
                .setTakenNames(takenNames)
                .setICs(ics)
                .transform();
        }

        reHijack(classUnit);
        
        doAfterTransformReplacement(classUnit);
    }

    private void loadLibraryImpl(ClassOrInterfaceDeclaration classUnit) {
        Set<String> uCall = new HashSet<>();
        List<Entry<String, String>> libCall = new LinkedList<>();
        List<Node> useless = new LinkedList<>();
        for (MethodCallExpr call: classUnit.findAll(MethodCallExpr.class)) {
            String methodSig = SignatureService.extractSignature(call);
            if ( ! LibraryImplLoader.isSupportedMethod(methodSig)) continue;
            Optional<Expression> scopeOp = call.getScope();
            if ( ! scopeOp.isPresent()) continue;
            Expression scope = scopeOp.get();
            String className = scope.toString();
            if ( ! LibraryImplLoader.isSupportedLib(className)) continue;
            useless.add(scope);
            String callSig = className+":"+methodSig;
            if (uCall.contains(callSig)) continue;
            libCall.add(new SimpleEntry<>(className, methodSig));
            uCall.add(callSig);
        }
        for (Node n: useless) n.remove();
        Set<String> usedLib = new HashSet<>();
        for (Entry<String, String> lc: libCall) {
            MethodDeclaration replacementCall = LibraryImplLoader.find(lc.getKey(), lc.getValue());
            if (replacementCall == null) {
                WoodLog.attach(WARNING, "The call's replacement not found: " + lc.getKey()+":"+lc.getValue());
                continue;
            }
            classUnit.getMembers().add(replacementCall.clone());
            usedLib.add(lc.getKey());
        }
        for (String lib: usedLib) {
            List<String> ims = LibraryImplLoader.getIms(lib);
            for (String im: ims) {
                cUnitWorker.addImportationIfAbsent(im);
            }
        }
    }

    private Map<String, String> removeSpiedVariables(List<FieldDeclaration> fields) {
        Map<String, String> spiedVars = new HashMap<>();
        for (FieldDeclaration f: fields) {
            if ( removeSpyAnno(f) ) {
                for (VariableDeclarator vari: f.getVariables()) {
                    String name = vari.getName().asString();
                    String type = vari.getType().asString();
                    spiedVars.put(name, type);
                }
            }
        }
        return spiedVars;
    }

    private boolean removeSpyAnno(FieldDeclaration f) {
        AnnotationExpr target = null;
        for (AnnotationExpr a: f.getAnnotations()) {
            if ( "Spy".equals(a.getName().asString()) ) {
                target = a;
                break;
            }
        }
        if (target == null) return false;
        target.remove();
        return true;
    }

    private Map<String, Set<String>> cookTheHijackedFields(List<FieldDeclaration> fields) {
        Map<String, Set<String>> output = new HashMap<>();
        List<Node> useless = new LinkedList<>();
        for (FieldDeclaration f: fields) {
            AnnotationExpr a = getHijackAnnotation(f);
            if (a == null) continue;

            String type = "";
            Set<String> names = new HashSet<>();
            for (VariableDeclarator v: f.getVariables()) {
                type = v.getType().asString();
                names.add(v.getName().asString());
            }
            Set<String> namesUnderType = output.get(type);
            if (namesUnderType == null) {
                output.put(type, names);
            } else {
                namesUnderType.addAll(names);
            }
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

    private Set<String> collectTakenNames(ClassOrInterfaceDeclaration classUnit) {
        Set<String> names = new HashSet<>();
        for (NameExpr n: classUnit.findAll(NameExpr.class)) {
            names.add(n.getName().asString());
        }
        return names;
    }

    private void eliminatePrepareBlock(List<MethodDeclaration> methods) {
        List<Entry<String, List<ReferenceType>>> preFuncs = new LinkedList<>();
        List<MethodDeclaration> testBlocks = new LinkedList<>();
        List<Node> useless = new LinkedList<>();
        for (MethodDeclaration mUnit: methods) {
            boolean isPrepareBlock = false;
            for (AnnotationExpr annotation: mUnit.getAnnotations()) {
                String annotationName = annotation.getName().asString();
                if ("Before".equals(annotationName) || "BeforeClass".equals(annotationName)) {
                    useless.add(annotation);
                    String pFunName = mUnit.getName().asString();
                    List<ReferenceType> exs = extractExs(mUnit);
                    preFuncs.add(new SimpleEntry<>(pFunName, exs));
                    isPrepareBlock = true;
                    break;
                } else if ("Test".equals(annotationName)){
                    testBlocks.add(mUnit);
                    break;
                }
            }
            if ( ! isPrepareBlock) continue;
            mUnit.getModifiers().add( new Modifier(Modifier.Keyword.PRIVATE) );
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

    public String[] findTypeByName(String type) {
        WoodLog.attach("find: delegate up to cUnit");
        return cUnitWorker.findTypeByName(type);
    }

    public String[] findTypeByOwner(String owner) {
        for (Entry<String, Set<String>> e: hijackedTypes.entrySet()) {
            if (e.getValue().contains(owner)) {
                return findTypeByName(e.getKey());
            }
        }
        String typename = spiedVars.get(owner);
        if (typename != null)
            return findTypeByName(typename);
        typename = vars.get(owner);
        if (typename != null)
            return findTypeByName(typename);
        WoodLog.attach("Found no type for owner ["+owner+"]");
        return null;
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
                for (String callSig: c.getCalleeSignatures()) {
                    tails.offer(callSig);
                }
                stacked.add(sig);
            }
            stacks.add( stackOfCurrentRoot );
        }
        return stacks;
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

    private void doAfterTransformReplacement(ClassOrInterfaceDeclaration classUnit) {
        replaceWhitebox(classUnit);
    }

    private void replaceWhitebox(ClassOrInterfaceDeclaration classUnit) {
        List<Node> whiteboxNodes = new LinkedList<>();
        List<String> repNames = new LinkedList<>();
        for (MethodCallExpr call: classUnit.findAll(MethodCallExpr.class)) {
            Optional<Expression> scopeOp = call.getScope();
            if ( ! scopeOp.isPresent()) continue;
            Expression scope = scopeOp.get();
            if (scope.toString().endsWith("Whitebox")) {
                repNames.add(WHITEBOX_REP);
                whiteboxNodes.add(scope);
            }
        }
        Iterator<String> repNameIte = repNames.iterator();
        for (Node rNode: whiteboxNodes) {
            String coName = repNameIte.next();
            rNode.replace(new NameExpr(coName));
        }
    }

    public String findTypeWithoutPackage(String subject) {
        String type = vars.get(subject);
        if (type == null) {
            type = findTypeInHijackedCollection(subject);
        }
        if (type == null) {
            type = spiedVars.get(subject);
        }
        if (type == null) {
            WoodLog.attach("Found no type of [" + subject + "] !");
        }
        return type;
    }

    private String findTypeInHijackedCollection(String subject) {
        for (Entry<String, Set<String>> e: hijackedTypes.entrySet()) {
            if (e.getValue().contains(subject)) {
                return e.getKey();
            }
        }
        return null;
    }
}

