package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;

import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyTreeResolver;
import com.strange.brokenapi.analysis.jdt.filter.deprecation.DeprecationFilter;
import com.strange.brokenapi.analysis.jdt.filter.error.FilterChain;
import com.strange.common.enums.JDKVersionEnum;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MavenProjectCompilationAnalyzer extends CompilationAnalyzer {
    private final Map<String, File> javaCodeFileMap;
    private final DependencyTreeResolver treeResolver;
    private List<File> javaCodeFileList;
    private final Map<String, List<IProblem>> codeCompilationProblemMap;
    private final List<String> dependencyJars;
    private final JDKVersionEnum jdkVersionEnum;
    private final List<String> sourcepathList;
    private CodeCompilationAnalyzer compilationAnalyzer;
    private boolean needAnalysis;

    public MavenProjectCompilationAnalyzer(MavenDependencyTreeResolver treeResolver, List<String> sourcepathList,
                                           Map<String, File> javaCodeFileMap) {
        this(treeResolver, sourcepathList, javaCodeFileMap, true);
    }

    public MavenProjectCompilationAnalyzer(MavenDependencyTreeResolver treeResolver, List<String> sourcepathList,
                                           Map<String, File> javaCodeFileMap, boolean needAnalysis) {
        this.javaCodeFileMap = javaCodeFileMap;
        this.treeResolver = treeResolver;
        this.jdkVersionEnum = treeResolver.getJdkVersionEnum();
        this.sourcepathList = sourcepathList;
        codeCompilationProblemMap = new HashMap<>();
        this.needAnalysis = needAnalysis;

        dependencyJars = treeResolver.getDependencyJars().stream()
                .map(File::getAbsolutePath).collect(Collectors.toList());

//        File projectDir = treeResolver.getProjectDir();
//        File classpath = FileUtil.file(projectDir, "target", "classes");
//        if (classpath.isDirectory()) sourcepathList.add(classpath.getAbsolutePath());
//        File testClasspath = FileUtil.file(projectDir, "target", "test-classes");
//        if (testClasspath.isDirectory()) sourcepathList.add(classpath.getAbsolutePath());

        analysisAllProjectJavaFile(treeResolver.getProjectDir());
    }

    private void analysisAllProjectJavaFile(File projectDir) {
        javaCodeFileList = new ArrayList<>();
        for (String sourceDirPath : SOURCE_DIR) {
            File sourceCodeDir = FileUtil.file(projectDir, sourceDirPath);

            if (sourceCodeDir.isDirectory()) {
                sourcepathList.add(sourceCodeDir.getAbsolutePath());
                FileUtil.walkFiles(sourceCodeDir, file -> {
                    if ("java".equals(FileUtil.getSuffix(file)) && !file.getAbsolutePath().contains("resources")) {
                        javaCodeFileList.add(file);
                    }
                });
            }
        }

        if (sourcepathList.isEmpty()) {
            return;
        }

        String[] sourceCodeArray = javaCodeFileList.stream().map(File::getAbsolutePath).toArray(String[]::new);
        this.compilationAnalyzer = new CodeCompilationAnalyzer(sourceCodeArray, dependencyJars, sourcepathList, jdkVersionEnum);
        if (needAnalysis) {
            analysisErrorAndWarning(compilationAnalyzer);
        }
    }

    private void analysisErrorAndWarning(CodeCompilationAnalyzer analyzer) {
        // first filter Error
        FilterChain filterChain = new FilterChain(javaCodeFileMap);
        List<Pair<File, IProblem>> errorResult = analyzer.analysisError();
        for (Pair<File, IProblem> errorPair : errorResult) {
            if (filterChain.filter(errorPair.getKey(), errorPair.getValue())) {
                addToProblemMap(errorPair);
            }
        }

        // Then filter Warning
        DeprecationFilter deprecationFilter = new DeprecationFilter(javaCodeFileMap);
        List<Pair<File, IProblem>> warningResult = analyzer.analysisWarning();
        for (Pair<File, IProblem> warningPair : warningResult) {
            if (deprecationFilter.filter(warningPair.getKey(), warningPair.getValue())) {
                addToProblemMap(warningPair);
            }
        }
    }

    private void addToProblemMap(Pair<File, IProblem> problemPair) {
        File codeFile = problemPair.getKey();
        List<IProblem> problemList = codeCompilationProblemMap.getOrDefault(codeFile.getAbsolutePath(), new ArrayList<>());
        problemList.add(problemPair.getValue());
        codeCompilationProblemMap.put(codeFile.getAbsolutePath(), problemList);
    }

    public DependencyTreeResolver getTreeResolver() {
        return treeResolver;
    }

    public List<File> getJavaCodeFileList() {
        return javaCodeFileList;
    }

    public Map<String, List<IProblem>> getCodeCompilationProblemMap() {
        return codeCompilationProblemMap;
    }

    public Map<String, CompilationUnit> getCodeFileASTMap() {
        return this.compilationAnalyzer.getCodeFileASTMap();
    }
}
