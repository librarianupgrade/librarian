package com.strange.brokenapi.analysis.jdt;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyModuleResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenModule;
import com.strange.common.utils.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import java.io.File;
import java.util.*;

@Slf4j
public class MavenModuleCompilationAnalyzer extends CompilationAnalyzer {
    private final MavenDependencyModuleResolver oldModuleResolver;
    private final MavenDependencyModuleResolver newModuleResolver;
    private final Map<String, File> javaCodeFileMap; // java fully qualified class name ---> its File Object
    private final Map<String, MavenProjectCompilationAnalyzer> compilationAnalyzerMap; // module name ---> complication analysis result
    private final File projectRootDir;
    private final List<String> sourceDirList;
    private final Map<String, CompilationUnit> oldCodeFileASTMap;
    private final Map<String, CompilationUnit> newCodeFileASTMap;
    private List<ErrorResult> errorResults;
    private List<DeprecationResult> deprecationResults;

    public MavenModuleCompilationAnalyzer(MavenDependencyModuleResolver oldModuleResolver, MavenDependencyModuleResolver newModuleResolver) {
        this.oldModuleResolver = oldModuleResolver;
        this.newModuleResolver = newModuleResolver;
        this.compilationAnalyzerMap = new HashMap<>();
        this.javaCodeFileMap = new HashMap<>();
        this.projectRootDir = newModuleResolver.getProjectDir();
        this.sourceDirList = new ArrayList<>();
        this.oldCodeFileASTMap = new HashMap<>();
        this.newCodeFileASTMap = new HashMap<>();
        this.errorResults = null;
        this.deprecationResults = null;

        MavenModule newRootModule = newModuleResolver.getRootModule();
        initMavenModuleEnv(newRootModule);
        traverseNewModules(newRootModule);
        traverseOldModules(oldModuleResolver.getRootModule());
    }

    private void initMavenModuleEnv(MavenModule module) {
        if (module.getModulePom()) {
            // if is a pom parent module
            for (MavenModule subModule : module.getSubModules()) {
                initMavenModuleEnv(subModule);
            }
        } else {
            // if is a jar module
            File moduleDir = module.getModuleDir();

            // process java code file (including source code file and test code file)
            for (String sourceDirPath : SOURCE_DIR) {
                File sourceDir = FileUtil.file(moduleDir.getAbsolutePath(), sourceDirPath);
                processDir(sourceDir);
            }
        }
    }

    private void processDir(File sourceDir) {
        if (sourceDir.isDirectory()) {
            sourceDirList.add(sourceDir.getAbsolutePath());
            FileUtil.walkFiles(sourceDir, file -> {
                if ("java".equals(FileUtil.getSuffix(file)) && !file.getAbsolutePath().contains("resources")) {
                    String relativePath = URLUtil.getRelativePath(file, sourceDir);
                    String className = relativePath.replace("\\", ".").replace("/", ".");
                    className = className.substring(0, relativePath.length() - 5); // remove .java suffix
                    javaCodeFileMap.put(removeClassNamePrefix(className), file);
                }
            });
        }
    }

    // remove test.java.xxx or main.java.xxx
    private String removeClassNamePrefix(String className) {
        boolean flag = false;
        Set<String> banned = Set.of("java", "main", "test");
        List<String> split = StrUtil.split(className, ".");
        List<String> classNameList = new ArrayList<>();
        for (String s : split) {
            if (!flag) {
                if (!banned.contains(s)) {
                    flag = true;
                    classNameList.add(s);
                }
            } else {
                classNameList.add(s);
            }

        }
        return StrUtil.join(".", classNameList);
    }

    private void traverseOldModules(MavenModule module) {
        if (module.getModulePom()) {
            for (MavenModule subModule : module.getSubModules()) {
                traverseOldModules(subModule);
            }
        } else {
            MavenProjectCompilationAnalyzer analyzer = new MavenProjectCompilationAnalyzer(module.getTreeResolver(), sourceDirList, javaCodeFileMap, false);
            Map<String, CompilationUnit> codeFileASTMap = analyzer.getCodeFileASTMap();
            oldCodeFileASTMap.putAll(codeFileASTMap);
        }
    }

    private void traverseNewModules(MavenModule module) {
        if (module.getModulePom()) {
            for (MavenModule subModule : module.getSubModules()) {
                traverseNewModules(subModule);
            }
        } else {
            MavenProjectCompilationAnalyzer analyzer = new MavenProjectCompilationAnalyzer(module.getTreeResolver(), sourceDirList, javaCodeFileMap);
            Map<String, CompilationUnit> codeFileASTMap = analyzer.getCodeFileASTMap();
            newCodeFileASTMap.putAll(codeFileASTMap);
            compilationAnalyzerMap.put(module.getModuleDir().getAbsolutePath(), analyzer);
        }
    }

    public Map<String, MavenProjectCompilationAnalyzer> getAnalyzerResultMap() {
        return compilationAnalyzerMap;
    }

    public List<ErrorResult> getErrorResults() {
        if (errorResults == null) {
            errorResults = getErrorResults(false);
        }
        return errorResults;
    }

    public List<ErrorResult> getErrorResults(boolean logging) {
        if (errorResults != null) {
            return errorResults;
        }

        Map<String, MavenProjectCompilationAnalyzer> analyzerResultMap = getAnalyzerResultMap();
        List<ErrorResult> errorResultList = new ArrayList<>();

        analyzerResultMap.forEach((modulePath, compilationAnalyzer) -> {
            Map<String, List<IProblem>> codeCompilationProblemMap = compilationAnalyzer.getCodeCompilationProblemMap();
            if (codeCompilationProblemMap.isEmpty()) return;

            if (logging) {
                log.info("## module: {}", modulePath);
            }
            String moduleName = URLUtil.getRelativePath(FileUtil.file(modulePath), projectRootDir);
            DependencyTreeResolver oldTreeResolver = oldModuleResolver.getModuleDependencyMap().get(moduleName);
            DependencyTreeResolver newTreeResolver = compilationAnalyzer.getTreeResolver();
            codeCompilationProblemMap.forEach((codeFile, problems) -> {
                StringBuilder sb = new StringBuilder();
                for (IProblem problem : problems) {
                    if (problem.isError()) {
                        ErrorResult errorResult = new ErrorResult();
                        errorResult.setModuleName(moduleName);

                        String[] arguments = problem.getArguments();
                        sb.append(Arrays.toString(arguments));
                        sb.append("   ");
                        sb.append(problem.getMessage());
                        sb.append(", ");

                        errorResult.setCodeFile(FileUtil.file(codeFile));
                        errorResult.setOldAST(oldCodeFileASTMap.get(codeFile));
                        errorResult.setNewAST(newCodeFileASTMap.get(codeFile));
                        errorResult.setOldTreeResolver(oldTreeResolver);
                        errorResult.setNewTreeResolver(newTreeResolver);
                        errorResult.setFilePath(codeFile);
                        errorResult.setErrorMessage(problem.getMessage());
                        errorResult.setErrorId(problem.getID());
                        errorResult.setErrorType(ProblemIdMapping.getErrorType(problem.getID()));
                        errorResult.setErrorLineNumber(problem.getSourceLineNumber());
                        errorResult.setOriginatingFileName(FileUtil.getName(new String(problem.getOriginatingFileName())));
                        errorResult.setArguments(Arrays.asList(problem.getArguments()));
                        errorResult.setProblem((DefaultProblem) problem);
                        errorResultList.add(errorResult);
                    }
                    if (logging) {
                        log.info("  ## {}: {}", codeFile, sb);
                    }
                }
            });
        });

        this.errorResults = errorResultList;
        return errorResultList;
    }


    public List<DeprecationResult> getDeprecationResults() {
        if (deprecationResults == null) {
            deprecationResults = getDeprecationResults(false);
        }
        return deprecationResults;
    }

    public List<DeprecationResult> getDeprecationResults(boolean logging) {
        if(deprecationResults != null) return deprecationResults;

        Map<String, MavenProjectCompilationAnalyzer> analyzerResultMap = getAnalyzerResultMap();
        List<DeprecationResult> deprecatedResultList = new ArrayList<>();

        analyzerResultMap.forEach((modulePath, compilationAnalyzer) -> {
            Map<String, List<IProblem>> codeCompilationProblemMap = compilationAnalyzer.getCodeCompilationProblemMap();
            if (codeCompilationProblemMap.isEmpty()) return;

            if (logging) {
                log.info("## module: {}", modulePath);
            }
            String moduleName = URLUtil.getRelativePath(FileUtil.file(modulePath), projectRootDir);
            DependencyTreeResolver oldTreeResolver = oldModuleResolver.getModuleDependencyMap().get(moduleName);
            DependencyTreeResolver newTreeResolver = compilationAnalyzer.getTreeResolver();
            codeCompilationProblemMap.forEach((codeFile, problems) -> {

                StringBuilder sb = new StringBuilder();
                for (IProblem problem : problems) {
                    if (problem.isWarning()) {
                        DeprecationResult deprecatedResult = new DeprecationResult();
                        deprecatedResult.setModuleName(moduleName);

                        String[] arguments = problem.getArguments();
                        sb.append(Arrays.toString(arguments));
                        sb.append("   ");
                        sb.append(problem.getMessage());
                        sb.append(", ");

                        deprecatedResult.setCodeFile(FileUtil.file(codeFile));
                        deprecatedResult.setOldTreeResolver(oldTreeResolver);
                        deprecatedResult.setNewTreeResolver(newTreeResolver);
                        deprecatedResult.setOldAST(oldCodeFileASTMap.get(codeFile));
                        deprecatedResult.setNewAST(newCodeFileASTMap.get(codeFile));
                        deprecatedResult.setFilePath(codeFile);
                        deprecatedResult.setDeprecatedMessage(problem.getMessage());
                        deprecatedResult.setDeprecatedId(problem.getID());
                        deprecatedResult.setDeprecatedType(ProblemIdMapping.getErrorType(problem.getID()));
                        deprecatedResult.setDeprecatedLineNumber(problem.getSourceLineNumber());
                        deprecatedResult.setOriginatingFileName(FileUtil.getName(new String(problem.getOriginatingFileName())));
                        deprecatedResult.setArguments(Arrays.asList(problem.getArguments()));
                        deprecatedResult.setProblem((DefaultProblem) problem);
                        deprecatedResultList.add(deprecatedResult);
                    }
                    if (logging) {
                        log.info("  ## {}: {}", codeFile, sb);
                    }
                }
            });
        });
        this.deprecationResults = deprecatedResultList;
        return deprecatedResultList;
    }

    public MavenDependencyModuleResolver getOldModuleResolver() {
        return oldModuleResolver;
    }

    public MavenDependencyModuleResolver getNewModuleResolver() {
        return newModuleResolver;
    }
}
