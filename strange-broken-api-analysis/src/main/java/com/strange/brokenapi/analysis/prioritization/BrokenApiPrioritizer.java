package com.strange.brokenapi.analysis.prioritization;

import cn.hutool.core.io.FileUtil;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.NotDirectedAcyclicGraphException;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class BrokenApiPrioritizer {

    private Graph<String, DefaultEdge> graph;

    private final File projectDir;

    private Map<String, File> classFileMap;

    private final List<BrokenApiUsage> brokenApiUsageList;

    private Map<String, List<BrokenApiUsage>> brokenApiUsageMap; // error file path ---> list of broken api usage

    private Queue<BrokenApiContainer> priorizedQueue;

    private List<String> topologicalClassList;

    public BrokenApiPrioritizer( File projectDir,  List<BrokenApiUsage> brokenApiUsageList) {
        this.projectDir = projectDir;
        this.brokenApiUsageList = brokenApiUsageList;
        this.priorizedQueue = null;
    }

    public Queue<BrokenApiContainer> prioritize() {
        if (priorizedQueue == null) {
            mapErrorResult();
            prioritizeClass(this.projectDir.toPath());
            generateBrokenApiQueue();
        }
        return priorizedQueue;
    }

    private void prioritizeClass(Path root) {
        // collect the all java file
        List<String> sourceFileList = new ArrayList<>();
        try {
            Files.walk(root)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> sourceFileList.add(path.toFile().getAbsolutePath()));
        } catch (IOException ignored) {
        }

        // construct the directed graph
        this.classFileMap = new HashMap<>();
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        ASTParser p = ASTParser.newParser(AST.getJLSLatest());
        p.setKind(ASTParser.K_COMPILATION_UNIT);
        p.setResolveBindings(true);
        p.setBindingsRecovery(true);
        p.setStatementsRecovery(true);
        String[] sourceCodePathArray = sourceFileList.toArray(new String[0]);

        String[] encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        p.setEnvironment(
                null,
                null,
                null,
                true
        );

        encodings = new String[sourceCodePathArray.length];
        Arrays.fill(encodings, "UTF-8");
        FileASTRequestor requester = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                ClassHierarchyVisitor classHierarchyVisitor = new ClassHierarchyVisitor();
                ast.accept(classHierarchyVisitor);
                Map<String, String> superClassNameMap = classHierarchyVisitor.getSuperClassNameMap();
                Map<String, List<String>> interfaceNameListMap = classHierarchyVisitor.getInterfaceNameListMap();

                for (Map.Entry<String, String> entry : superClassNameMap.entrySet()) {
                    String currentClassName = entry.getKey();
                    String superClassName = entry.getValue();
                    if (currentClassName != null && superClassName != null) {
                        graph.addVertex(currentClassName);
                        graph.addVertex(superClassName);
                        graph.addEdge(superClassName, currentClassName);
                    }
                }

                for (Map.Entry<String, List<String>> entry : interfaceNameListMap.entrySet()) {
                    String currentClassName = entry.getKey();
                    List<String> interfaceNameList = entry.getValue();
                    if (currentClassName != null && interfaceNameList != null) {
                        graph.addVertex(currentClassName);
                        for (String interfaceName : interfaceNameList) {
                            graph.addVertex(interfaceName);
                            graph.addEdge(interfaceName, currentClassName);
                        }
                    }
                }

                List<String> classNameList = classHierarchyVisitor.getClassNameList();
                if (classNameList != null) {
                    for (String currentClassName : classNameList) {
                        classFileMap.put(currentClassName, FileUtil.file(sourceFilePath));
                    }
                }
            }
        };
        p.createASTs(sourceCodePathArray, encodings, new String[]{}, requester, new NullProgressMonitor());

        // topological order
        try {
            TopologicalOrderIterator<String, DefaultEdge> topologicalOrderIterator = new TopologicalOrderIterator<>(graph);
            topologicalClassList = new ArrayList<>();
            while (topologicalOrderIterator.hasNext()) {
                topologicalClassList.add(topologicalOrderIterator.next());
            }
        } catch (NotDirectedAcyclicGraphException e) {
            CycleDetector<String, DefaultEdge> detector =
                    new CycleDetector<>(graph);
            boolean hasCycle = detector.detectCycles();
            if (hasCycle) {
                log.error("Cycle detected in the topology graph:", e);
                Set<String> verticesInCycle = detector.findCycles();
                log.error("Vertices involved in the cycle:");
                for (String v : verticesInCycle) {
                    log.error("  - {}", v);
                }
            }
            System.exit(1);
        }
    }

    private void mapErrorResult() {
        brokenApiUsageMap = new HashMap<>();
        for (BrokenApiUsage brokenApiUsage : brokenApiUsageList) {
            String errorFilePath = brokenApiUsage.getErrorResult().getFilePath();
            List<BrokenApiUsage> brokenApiList = brokenApiUsageMap.getOrDefault(errorFilePath, new ArrayList<>());
            brokenApiList.add(brokenApiUsage);
            brokenApiUsageMap.put(errorFilePath, brokenApiList);
        }
    }

    private void generateBrokenApiQueue() {
        priorizedQueue = new LinkedList<>();
        for (String className : topologicalClassList) {
            if (classFileMap.containsKey(className)) {
                File javaCodeFile = classFileMap.get(className);
                if (brokenApiUsageMap.containsKey(javaCodeFile.getAbsolutePath())) {
                    List<BrokenApiUsage> brokenApiUsages = brokenApiUsageMap.get(javaCodeFile.getAbsolutePath());
                    BrokenApiContainer brokenApiContainer = new BrokenApiContainer(javaCodeFile, brokenApiUsages);
                    priorizedQueue.add(brokenApiContainer);
                }
            }
        }
    }
}
