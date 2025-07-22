package com.strange.brokenapi.analysis;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.strange.brokenapi.analysis.dependency.DependencyTreeResolver;
import com.strange.brokenapi.analysis.dependency.maven.MavenDependencyModuleResolver;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.jdt.MavenModuleCompilationAnalyzer;
import com.strange.brokenapi.analysis.jdt.locate.MavenModuleProblemAnalyzer;
import com.strange.common.config.GlobalConfig;
import com.strange.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jface.text.BadLocationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
public class BrokenApiExtractor {

    public static List<BrokenApiUsage> extractBrokenApi(File projectDir, File cacheDir) {
        if (!projectDir.isDirectory()) {
            throw new IllegalArgumentException("The input path is not a directory: " + projectDir.getAbsolutePath());
        }

        InputProjectContext projectContext = new InputProjectContext(projectDir);

        File changePomFile = projectContext.getChangePomFile();
        MavenDependencyModuleResolver oldResolver = TimeUtil.runTask(() -> {
            log.info("Starting to process the old POM file");
            MavenDependencyModuleResolver resolver = null;
            File oldPomFile = projectContext.getOldPomFile();
            FileUtil.copy(oldPomFile, changePomFile, true);
            try {
                resolver = new MavenDependencyModuleResolver(projectContext.getProjectRootDir(),
                        projectContext.getJdkVersion(), false, "before-dependency");
            } catch (Exception e) {
                log.error("OldMavenDependencyModuleResolverFail: ", e);
                return resolver;
            }
            return resolver;
        }, "process-old-pom-file");

        if (oldResolver == null) {
            throw new RuntimeException("Failed to process the old POM file. Resolver is null.");
        }

        // Process New POM File
        MavenDependencyModuleResolver newResolver = TimeUtil.runTask(() -> {
            log.info("Starting to process the new POM file");
            MavenDependencyModuleResolver resolver = null;
            File newPomFile = projectContext.getNewPomFile();
            FileUtil.copy(newPomFile, changePomFile, true);
            try {
                resolver = new MavenDependencyModuleResolver(projectContext.getProjectRootDir(),
                        projectContext.getJdkVersion(), false, "after-dependency");
            } catch (Exception e) {
                log.error("NewMavenDependencyModuleResolverFail: ", e);
                return resolver;
            }
            return resolver;
        }, "process-new-pom-file");

        if (newResolver == null) {
            throw new RuntimeException("Failed to process the new POM file. Resolver is null.");
        }

        // Generate Compilation Analyzer
        MavenModuleCompilationAnalyzer moduleCompilationAnalyzer = TimeUtil.runTask(() -> {
            MavenModuleCompilationAnalyzer analyzer = new MavenModuleCompilationAnalyzer(oldResolver, newResolver);
            Map<String, DependencyTreeResolver> moduleDependencyMap = oldResolver.getModuleDependencyMap();

            List<ErrorResult> errorResults = analyzer.getErrorResults();
            for (ErrorResult errorResult : errorResults) {
                String moduleName = errorResult.getModuleName();
                if (!moduleDependencyMap.containsKey(moduleName)) {
                    throw new RuntimeException("Module '" + moduleName + "' not found in moduleDependencyMap.");
                }
            }
            // save the first result of compilation
            saveInitialErrorResults(errorResults, cacheDir);
            return analyzer;
        }, "generate-compilation-analyzer");

        // Identify Broken Api Usage
        List<ErrorResult> errorResultList = TimeUtil.runTask(() -> {
            List<ErrorResult> errorResults = moduleCompilationAnalyzer.getErrorResults();
            new MavenModuleProblemAnalyzer(moduleCompilationAnalyzer, projectContext.getFixType()); // need to be invoked to get the root cause of the dependency and broken api signature
            return errorResults;
        }, "broken-api-usage-identification");

        // Extract Broken Api Usage
        return TimeUtil.runTask(() -> {
            List<BrokenApiUsage> brokenApiUsageList = new ArrayList<>();
            for (ErrorResult errorResult : errorResultList) {
                BrokenApiUsage brokenApiUsage = null;
                try {
                    brokenApiUsage = BrokenApiUsage.extractFromErrorResult(errorResult);
                } catch (BadLocationException e) {
                    log.error("BrokenAPIUsageExtractionError: ", e);
                }
                brokenApiUsageList.add(brokenApiUsage);
            }
            return brokenApiUsageList;
        }, "broken-api-usage-extraction");
    }

    public static List<BrokenApiUsage> extractBrokenApi(String projectDir) {
        return extractBrokenApi(FileUtil.file(projectDir), FileUtil.file(projectDir, GlobalConfig.CacheDirName));
    }

    private static void saveInitialErrorResults(List<ErrorResult> errorResults, File cacheDir) {
        File initialBrokenApiUsageFile = FileUtil.file(cacheDir, "initial_broken_api_usages.json");
        if (!FileUtil.exist(initialBrokenApiUsageFile)) {
            new cn.hutool.core.io.file.FileWriter(initialBrokenApiUsageFile).write(JSONUtil.toJsonPrettyStr(errorResults));
        }
    }
}
