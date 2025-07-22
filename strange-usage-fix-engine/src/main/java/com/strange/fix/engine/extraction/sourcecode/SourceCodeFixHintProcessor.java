package com.strange.fix.engine.extraction.sourcecode;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.jdt.locate.ErrorProblemLocation;
import com.strange.common.utils.CodeUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.extraction.hint.CodeMapping;
import com.strange.fix.engine.extraction.hint.CodeMappingComparator;
import com.strange.fix.engine.extraction.hint.MigrationCaseHint;
import com.strange.fix.engine.extraction.sourcecode.localization.ApiLocationFactory;
import com.strange.fix.engine.extraction.sourcecode.localization.ApiLocator;
import com.strange.fix.engine.extraction.sourcecode.localization.entity.ApiLocation;
import com.strange.fix.engine.extraction.sourcecode.mapping.MappingSourceEnum;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringAnalyzer;
import com.strange.fix.engine.extraction.sourcecode.mapping.RefactoringAnalyzerFactory;
import com.strange.fix.engine.formatter.CodeFormatter;
import com.strange.fix.engine.konwledge.LibraryDatabaseManager;
import com.strange.fix.engine.similiarity.CodeSimilarityResult;
import com.strange.fix.engine.similiarity.SimilarityService;
import com.strange.fix.engine.slicing.CodeSlicer;
import com.strange.fix.engine.slicing.MethodSlicingHelper;
import com.strange.fix.engine.slicing.SlicingFactory;
import com.strange.fix.engine.slicing.SlicingResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@Slf4j
public class SourceCodeFixHintProcessor {

    private static final Integer MAX_SLICING_THREAD_NUMBER = 20;

    private static final Integer MAX_SLICED_CODE_NUMBER = 10; // control the number of the sliced code, CPU workstation too slow

    private final BrokenApiUsage brokenApiUsage;

    private final File cacheDir;

    private final Integer fixRound;

    private final LibraryDatabaseManager libraryDatabaseManager;

    public SourceCodeFixHintProcessor(BrokenApiUsage brokenApiUsage, File libraryDatabaseDir,
                                      File cacheDir, Integer fixRound) {
        this.brokenApiUsage = brokenApiUsage;
        this.cacheDir = cacheDir;
        this.fixRound = fixRound;
        this.libraryDatabaseManager = new LibraryDatabaseManager(libraryDatabaseDir);
    }

    private List<ApiLocation> locateApiInOldLibrary(ApiSignature apiSignature, MappingSourceEnum mappingSourceEnum) {
        // locate the api used in old library
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String oldVersion = errorProblemLocation.getOldVersion();

        // resolve old version of library
        File oldLibrarySourceDir = switch (mappingSourceEnum) {
            case SOURCE_CODE -> libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, oldVersion);
            case TESTCASE_CODE -> libraryDatabaseManager.getLibraryTestcaseDir(groupId, artifactId, oldVersion);
        };

        if (FileUtil.exist(oldLibrarySourceDir)) {
            ApiLocator originalApiLocator = ApiLocationFactory.getApiLocator(oldLibrarySourceDir, apiSignature.getBrokenApiType(),
                    apiSignature);
            if (originalApiLocator == null) {
                log.warn(String.format(
                        "Could not locate original API of type '%s' with signature '%s' in %s",
                        brokenApiUsage.getBrokenApiType(),
                        brokenApiUsage.getApiSignature(),
                        oldLibrarySourceDir.getAbsolutePath()
                ));
                return new ArrayList<>();
            }
            if (mappingSourceEnum == MappingSourceEnum.SOURCE_CODE) {
                return originalApiLocator.retrieveApiLocation();
            } else {
                File librarySourceDir = libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, oldVersion);
                return originalApiLocator.retrieveApiLocation(List.of(librarySourceDir.getAbsolutePath()));
            }
        } else {
            return new ArrayList<>();
        }
    }

    private List<SlicingResultPair> sliceLibraryCodeInOldLibrary(List<ApiLocation> originalApiLocations) {
        List<SlicingResultPair> oldLibrarySlicedCodeList = new CopyOnWriteArrayList<>();
        ExecutorService executor = ThreadUtil.newExecutor(Integer.min(MAX_SLICING_THREAD_NUMBER, originalApiLocations.size()));
        CountDownLatch latch = new CountDownLatch(originalApiLocations.size());
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String oldVersion = errorProblemLocation.getOldVersion();

        File oldLibraryJarFile = libraryDatabaseManager.getLibraryJarFile(groupId, artifactId, oldVersion);
        File oldLibraryTestcaseJarFile = libraryDatabaseManager.getLibraryTestcaseJarFile(groupId, artifactId, oldVersion);

        // init the slicing environment
        List<File> jarFileList = new ArrayList<>();
        if (FileUtil.exist(oldLibraryJarFile)) jarFileList.add(oldLibraryJarFile);
        if (FileUtil.exist(oldLibraryTestcaseJarFile)) jarFileList.add(oldLibraryTestcaseJarFile);

        CodeSlicer.initSlicingEnvironment(jarFileList);


        // multi-thread run slicing
        for (ApiLocation originalApiLocation : originalApiLocations) {
            executor.submit(() -> {
                try {
                    CodeSlicer libraryCodeSlicer = SlicingFactory.getCodeSlicer(
                            originalApiLocation.getLocatedFile(),
                            List.of(originalApiLocation.getStartLineNumber()),
                            oldLibraryJarFile,
                            originalApiLocation.getTargetApiSignature());

                    SlicingResult slicingResult = libraryCodeSlicer.slicedCodeWithSyntaxCompletion();
                    SlicingResultPair slicingResultPair = new SlicingResultPair(originalApiLocation, slicingResult);
                    oldLibrarySlicedCodeList.add(slicingResultPair);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("MultiThreadRunSlicingError: ", e);
        } finally {
            executor.shutdown();
            log.info("OldVersionCodeSlicingFinish");
        }

        Collections.sort(oldLibrarySlicedCodeList);
        int end = Math.min(MAX_SLICED_CODE_NUMBER, oldLibrarySlicedCodeList.size());
        return oldLibrarySlicedCodeList.subList(0, end);
    }

    private List<ApiLocation> locateApiInNewLibrary(ApiSignature mappedApiSignature, MappingSourceEnum mappingSource) {
        // locate the mapped API in new library
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String newVersion = errorProblemLocation.getNewVersion();

        File newLibrarySourceDir = switch (mappingSource) {
            case SOURCE_CODE -> libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, newVersion);
            case TESTCASE_CODE -> libraryDatabaseManager.getLibraryTestcaseDir(groupId, artifactId, newVersion);
        };

        if (FileUtil.exist(newLibrarySourceDir)) {
            ApiLocator newApiLocator = ApiLocationFactory.getApiLocator(newLibrarySourceDir, mappedApiSignature.getBrokenApiType(), mappedApiSignature);
            if (newApiLocator == null) {
                log.warn(String.format(
                        "Could not locate mapped API of type '%s' with signature '%s' in %s for version %s",
                        brokenApiUsage.getBrokenApiType(),
                        mappedApiSignature,
                        newLibrarySourceDir.getAbsolutePath(),
                        newVersion
                ));
                return new ArrayList<>();
            }
            if (mappingSource == MappingSourceEnum.SOURCE_CODE) {
                return newApiLocator.retrieveApiLocation();
            } else {
                File librarySourceDir = libraryDatabaseManager.getLibrarySourceDir(groupId, artifactId, newVersion);
                return newApiLocator.retrieveApiLocation(List.of(librarySourceDir.getAbsolutePath()));
            }
        } else {
            return new ArrayList<>();
        }
    }

    private List<SlicingResult> sliceLibraryCodeInNewLibrary(List<ApiLocation> newApiLocations) {
        // new version code slicing
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String newVersion = errorProblemLocation.getNewVersion();

        File newLibraryJarFile = libraryDatabaseManager.getLibraryJarFile(groupId, artifactId, newVersion);
        File newLibraryTestcaseJarFile = libraryDatabaseManager.getLibraryTestcaseJarFile(groupId, artifactId, newVersion);

        // init the slicing environment
        List<File> jarFileList = new ArrayList<>();
        if (FileUtil.exist(newLibraryJarFile)) jarFileList.add(newLibraryJarFile);
        if (FileUtil.exist(newLibraryTestcaseJarFile)) jarFileList.add(newLibraryTestcaseJarFile);
        CodeSlicer.initSlicingEnvironment(jarFileList);


        // multi-thread run slicing
        List<SlicingResult> newLibrarySlicedCodeList = new CopyOnWriteArrayList<>();
        ExecutorService executor = ThreadUtil.newExecutor(Integer.min(MAX_SLICING_THREAD_NUMBER, newApiLocations.size()));
        CountDownLatch latch = new CountDownLatch(newApiLocations.size());
        for (ApiLocation newApiLocation : newApiLocations) {
            executor.submit(() -> {
                try {
                    CodeSlicer libraryCodeSlicer = SlicingFactory.getCodeSlicer(
                            newApiLocation.getLocatedFile(),
                            List.of(newApiLocation.getStartLineNumber()),
                            newLibraryJarFile,
                            newApiLocation.getTargetApiSignature());
                    SlicingResult slicingResult = libraryCodeSlicer.slicedCodeWithSyntaxCompletion();
                    newLibrarySlicedCodeList.add(slicingResult);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("MultiThreadRunSlicingError: ", e);
        } finally {
            executor.shutdown();
            log.info("NewVersionCodeSlicingFinish");
        }
        return newLibrarySlicedCodeList;
    }

    @Data
    static class SlicingResultPair implements Comparable<SlicingResultPair> {
        private ApiLocation apiLocation;

        private SlicingResult slicingResult;

        public SlicingResultPair(ApiLocation apiLocation, SlicingResult slicingResult) {
            this.apiLocation = apiLocation;
            this.slicingResult = slicingResult;
        }

        @Override
        public int compareTo(SlicingResultPair other) {
            String slicedCode = this.getSlicingResult().getSlicedCode();
            String otherSlicedCode = other.getSlicingResult().getSlicedCode();
            return slicedCode.length() - otherSlicedCode.length();
        }
    }

    private RetrieveCodeResult retrieveCodeMapping(BrokenApiUsage brokenApiUsage, String clientSlicedCode,
                                                   List<SlicingResultPair> originalSlicingResultList, MappingSourceEnum mappingSource) {
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String oldVersion = errorProblemLocation.getOldVersion();
        String newVersion = errorProblemLocation.getNewVersion();

        List<CodeMapping> codeMappingList = new ArrayList<>();
        List<CodeSimilarityResult> oldLibraryCodeSimilarityList = new ArrayList<>();

        RefactoringAnalyzer refactoringAnalyzer = RefactoringAnalyzerFactory.getRefactoringAnalyzer(libraryDatabaseManager, groupId,
                artifactId, oldVersion, newVersion, mappingSource);

        for (SlicingResultPair slicingResultPair : originalSlicingResultList) {
            ApiLocation apiLocation = slicingResultPair.getApiLocation();
            ApiSignature locatedApiSignature = apiLocation.getLocatedApiSignature();
            if (locatedApiSignature == null) {
                continue;
            }

            SlicingResult slicingResult = slicingResultPair.getSlicingResult();
            String oldLibrarySlicedCode = slicingResult.getSlicedCode();
            CodeSimilarityResult codeSimilarityResult = SimilarityService.calculateSimilarity(clientSlicedCode, oldLibrarySlicedCode);
            oldLibraryCodeSimilarityList.add(codeSimilarityResult);


            ApiSignature mappedLocatedApiSignature = refactoringAnalyzer.apiMapping(locatedApiSignature);

            List<ApiLocation> apiDefLocations = locateApiInNewLibrary(mappedLocatedApiSignature, mappingSource);
            if (CollUtil.isNotEmpty(apiDefLocations)) {
                if (apiDefLocations.size() != 1) {
                    log.info("MappingOccurOneToMany");
                }

                ApiLocation newApiDefLocation = apiDefLocations.get(0);

                List<String> targetStatementList = new ArrayList<>();
                List<Integer> originalTaintedCode = slicingResultPair.getSlicingResult().getTaintedLineNumbers();

                for (Integer lineNumber : originalTaintedCode) {
                    String targetStatement = JDTUtil.getStatementByLineNumber(apiLocation.getLocatedFile(), lineNumber);
                    targetStatement = CodeUtil.removeJavaDoc(targetStatement);
                    if (StrUtil.isNotEmpty(targetStatement)) {
                        targetStatementList.add(targetStatement);
                    }
                }

                StatementSimilarityMapper statementSimilarityMapper = new StatementSimilarityMapper(newApiDefLocation, targetStatementList);
                List<Integer> similarStatementLineNumberList = statementSimilarityMapper.getSimilarStatements();
                if (CollUtil.isEmpty(similarStatementLineNumberList)) continue;

                String refactoredCode = MethodSlicingHelper.trimWithLineNumbers(newApiDefLocation.getLocatedFile(), similarStatementLineNumberList);

                CodeMapping codeMapping = new CodeMapping(slicingResult.getSlicedCode(),
                        refactoredCode, codeSimilarityResult.getSimilarityScore());
                codeMappingList.add(codeMapping);
            }
        }
        RetrieveCodeResult retrieveCodeResult = new RetrieveCodeResult();
        retrieveCodeResult.setCodeMappingList(codeMappingList);
        retrieveCodeResult.setOldLibraryCodeSimilarityList(oldLibraryCodeSimilarityList);
        return retrieveCodeResult;
    }

    public MigrationCaseHint extractHint() {
        ErrorProblemLocation errorProblemLocation = brokenApiUsage.getErrorResult().getErrorProblemLocation();
        if (errorProblemLocation == null) {
            return null;
        }

        String groupId = errorProblemLocation.getGroupId();
        String artifactId = errorProblemLocation.getArtifactId();
        String oldVersion = errorProblemLocation.getOldVersion();
        String newVersion = errorProblemLocation.getNewVersion();
        String clientSlicedCode = brokenApiUsage.getSlicedBrokenCode();


        MigrationCaseHint migrationCaseHint = new MigrationCaseHint();
        List<CodeMapping> codeMappingList = new ArrayList<>();

        // broken API Mapping
        RefactoringAnalyzer refactoringAnalyzer = RefactoringAnalyzerFactory.getRefactoringAnalyzer(libraryDatabaseManager, groupId,
                artifactId, oldVersion, newVersion, MappingSourceEnum.SOURCE_CODE);
        ApiSignature mappedApiSignature = refactoringAnalyzer.apiMapping(brokenApiUsage.getApiSignature());
        migrationCaseHint.setMappedApiSignature(mappedApiSignature);

        for (MappingSourceEnum mappingSource : MappingSourceEnum.values()) {
            // process the old library
            List<ApiLocation> apiLocations = locateApiInOldLibrary(brokenApiUsage.getApiSignature(), mappingSource);
            List<SlicingResultPair> oldLibrarySlicingResult = sliceLibraryCodeInOldLibrary(apiLocations);

            // retrieve code mapping example
            RetrieveCodeResult retrieveCodeResult = retrieveCodeMapping(brokenApiUsage, clientSlicedCode, oldLibrarySlicingResult, mappingSource);

            List<CodeMapping> sourceCodeMappingList = retrieveCodeResult.getCodeMappingList();
            if (CollUtil.isNotEmpty(sourceCodeMappingList)) {
                codeMappingList.addAll(sourceCodeMappingList);
            }
        }

        codeMappingList.sort(new CodeMappingComparator());

        // save the migration cases to cache directory
        saveMigrationCases(clientSlicedCode, codeMappingList, cacheDir);
        migrationCaseHint.setCodeMappingList(codeMappingList);
        migrationCaseHint.setClientSlicedCode(clientSlicedCode);

        return migrationCaseHint;

        // process the new library
//        List<ApiLocation> newApiLocations = locateApiInNewLibrary(mappedApiSignature);
//        List<SlicingResult> newLibrarySlicedResultList = sliceLibraryCodeInNewLibrary(newApiLocations);
//        List<String> newLibrarySlicedCodeList = newLibrarySlicedResultList.stream()
//                .map(SlicingResult::getSlicedCode).toList();

        // choose the best sliced code in old version
//        List<CodeSimilarityResult> oldCodeSimilarityResults = retrieveCodeResult.getOldLibraryCodeSimilarityList();
//        oldCodeSimilarityResults.sort(new CodeSimilarityResultComparator());
//        double bestScore = -1.0;
//        String oldLibrarySlicedCode = null;
//        for (CodeSimilarityResult codeSimilarityResult : oldCodeSimilarityResults) {
//            if (codeSimilarityResult.getSimilarityScore() > bestScore) {
//                bestScore = codeSimilarityResult.getSimilarityScore();
//                oldLibrarySlicedCode = codeSimilarityResult.getCode();
//            }
//        }
//
//        // calculate the similarity of sliced code in new version
//        List<CodeSimilarityResult> newCodeSimilarityResults = SimilarityService.batchCalculateSimilarity(clientSlicedCode, newLibrarySlicedCodeList);
//        newCodeSimilarityResults.sort(new CodeSimilarityResultComparator());
//        List<String> codeList = newCodeSimilarityResults.stream().map(CodeSimilarityResult::getCode).toList();
    }

    private void saveMigrationCases(String clientSlicedCode, List<CodeMapping> codeMappingList, File cacheDir) {
        File slicedClientCodeFile = FileUtil.file(cacheDir, "client_code_" + fixRound + ".java.cache");
        File codeMappingFile = FileUtil.file(cacheDir, "code_mapping_" + fixRound + ".json");
        new FileWriter(slicedClientCodeFile).write(new CodeFormatter(clientSlicedCode).startFormat());
        new FileWriter(codeMappingFile).write(JSONUtil.toJsonPrettyStr(codeMappingList));
    }
}
