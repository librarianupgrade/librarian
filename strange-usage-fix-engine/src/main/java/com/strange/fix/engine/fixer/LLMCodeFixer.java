package com.strange.fix.engine.fixer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.BrokenApiContentExtractor;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.common.utils.CodeMappingUtil;
import com.strange.common.utils.JDTUtil;
import com.strange.fix.engine.FixEngine;
import com.strange.fix.engine.FixFileProcessor;
import com.strange.fix.engine.FixResult;
import com.strange.fix.engine.check.PatchChecker;
import com.strange.fix.engine.enums.FixEnum;
import com.strange.fix.engine.extraction.hint.FixHint;
import com.strange.fix.engine.extraction.hint.JavaDocHint;
import com.strange.fix.engine.extraction.hint.MigrationCaseHint;
import com.strange.fix.engine.extraction.javadoc.JavadocFixHintProcessor;
import com.strange.fix.engine.extraction.sourcecode.SourceCodeFixHintProcessor;
import com.strange.fix.engine.formatter.CodeFormatter;
import com.strange.fix.engine.llm.CodeElementMapper;
import com.strange.fix.engine.llm.FixPromptGenerator;
import com.strange.fix.engine.llm.LLMFixModel;
import com.strange.fix.engine.llm.LLMFixResult;
import com.strange.fix.engine.llm.visitor.CodeMergeVisitor;
import com.strange.fix.engine.slicing.CodeSlicer;
import com.strange.fix.engine.slicing.SlicingFactory;
import com.strange.fix.engine.slicing.SlicingResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.util.*;

@Slf4j
public class LLMCodeFixer extends CodeFixer {
    private static Integer TOTAL_FIX_ROUND = 0;

    private static final Integer MAX_ADDED_NUMBER = 4;

    private static final Integer MAX_FIX_DEPTH = 3;

    private static final String CLASSPATH_DIR = "target/classes";

    private static final String TESTCASE_CLASSPATH_DIR = "target/test-classes";

    private static final String LLM_CODE_FILE_NAME = "llm_code_{}_{}.java.cache";

    private static final String PROMPT_CODE_FILE_NAME = "prompt_code_{}_{}.java.cache";

    @Override
    public FixResult fix(FixFileProcessor fixFileProcessor, LLMFixModel llmFixModel, File rootDir, File projectDir,
                         BrokenApiUsage brokenApiUsage, List<BrokenApiUsage> brokenApiUsageList,
                         File libraryDatabaseDir, Integer maxRetryCount, File cacheDir, int fixDepth) {
        if (fixDepth > MAX_FIX_DEPTH) {
            return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                    .brokenApiUsageList(brokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
        }

        if (brokenApiUsage == null || brokenApiUsage.getApiSignature() == null) {
            return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                    .brokenApiUsageList(brokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
        }

        ErrorResult errorResult = brokenApiUsage.getErrorResult();
        File codeFile = errorResult.getCodeFile();
        File moduleDir;
        if (StringUtils.isEmpty(errorResult.getModuleName())) {
            moduleDir = projectDir;
        } else {
            moduleDir = FileUtil.file(projectDir, errorResult.getModuleName());
        }

        File clientJarFile;
        if (checkIsSourceCode(codeFile)) {
            clientJarFile = FileUtil.file(moduleDir, CLASSPATH_DIR);
        } else {
            clientJarFile = FileUtil.file(moduleDir, TESTCASE_CLASSPATH_DIR);
        }
        // client project slicing
        // init the slicing environment
        File processedCodeFile = fixFileProcessor.getFile(codeFile);
        extractClientUsage(processedCodeFile, brokenApiUsage, clientJarFile, codeFile);

        // copy the broken api file and prompt code file
        FileUtil.copy(processedCodeFile, new File(cacheDir, processedCodeFile.getName() + ".cache"), true);
        File promptCodeFile = FileUtil.file(cacheDir, StrUtil.format(PROMPT_CODE_FILE_NAME, FileNameUtil.getPrefix(processedCodeFile), IdUtil.getSnowflakeNextIdStr()));
        new FileWriter(promptCodeFile).write(new CodeFormatter(brokenApiUsage.getBrokenContent()).startFormat());

        // get fix hint in migration case (including broken API mapping and broken API caller mapping)
        MigrationCaseHint migrationCaseHint = retrieveApiUsageChangeHit(brokenApiUsage, libraryDatabaseDir, cacheDir);

        // get fix hint in java document
        JavaDocHint javaDocHint = retrieveJavaDocHint(brokenApiUsage, libraryDatabaseDir);

        FixHint fixHint = new FixHint(javaDocHint, migrationCaseHint);

        int fixRound = 0;
        FixPromptGenerator fixPromptGenerator = new FixPromptGenerator(brokenApiUsage, fixHint);
        FixResult fixResult;
        FixFileProcessor clonedFixFileProcessor = fixFileProcessor.clone();

        while (fixRound < maxRetryCount) {
            fixRound += 1;
            TOTAL_FIX_ROUND += 1;
            LLMFixResult llmFixResult = callLLM(llmFixModel, fixPromptGenerator, brokenApiUsage, cacheDir);
            String fixCode = llmFixResult.getFixCode();
            if (StrUtil.isEmptyOrUndefined(fixCode)) {
                log.error("LLMAnswerIsNull");
                continue;
            }
            File llmCodeFile = llmFixResult.getFixCodeFile();

            // merge previous code file with llm code file
            File fixCodeFile = mergeToFile(brokenApiUsage, clonedFixFileProcessor.getFile(codeFile.getAbsolutePath()), promptCodeFile, llmCodeFile, cacheDir, FileNameUtil.getPrefix(codeFile));
            // check the fix result
            fixResult = checkFixResult(clonedFixFileProcessor, rootDir, brokenApiUsage, brokenApiUsageList, fixCodeFile, codeFile, cacheDir);
            if (fixResult.isFixed()) {
                if (fixCodeFile != null) {
                    clonedFixFileProcessor.addFixFile(codeFile.getAbsolutePath(), fixCodeFile);
                }

                List<BrokenApiUsage> currentBrokenApiUsageList = fixResult.getBrokenApiUsageList();
                List<BrokenApiUsage> addedBrokenApiUsageList = fixResult.getAddedBrokenApiUsageList();
                boolean fixSuccessStatus = false;
                if (CollUtil.isNotEmpty(addedBrokenApiUsageList)) {
                    // if it has newly added broken API usage
                    if (addedBrokenApiUsageList.size() < MAX_ADDED_NUMBER) {
                        boolean allAddedBrokenApiUsageFixed = true;
                        for (BrokenApiUsage addedBrokenApiUsage : addedBrokenApiUsageList) {
                            if (addedBrokenApiUsage.getBrokenApiType() == ApiTypeEnum.IMPORT) continue;
                            ArrayList<BrokenApiUsage> nextBrokenApiUsageList = new ArrayList<>(currentBrokenApiUsageList);
                            nextBrokenApiUsageList.addAll(addedBrokenApiUsageList);
                            FixResult nextFixResult = FixEngine.fix(clonedFixFileProcessor, llmFixModel, rootDir, projectDir, addedBrokenApiUsage, nextBrokenApiUsageList,
                                    libraryDatabaseDir, maxRetryCount, cacheDir, fixDepth + 1);
                            List<BrokenApiUsage> nextAddedBrokenApiUsageList = nextFixResult.getAddedBrokenApiUsageList();
                            if (CollUtil.isNotEmpty(nextAddedBrokenApiUsageList)) {
                                allAddedBrokenApiUsageFixed = false;
                                break;
                            }
                        }
                        if (allAddedBrokenApiUsageFixed) {
                            log.info("All added broken api usage fixed");
                            fixFileProcessor.setTempFixFileMap(clonedFixFileProcessor.getTempFixFileMap());
                            fixSuccessStatus = true;
                        }
                    }
                } else {
                    // if it is having no newly added broken API usage
                    fixFileProcessor.setTempFixFileMap(clonedFixFileProcessor.getTempFixFileMap());
                    fixSuccessStatus = true;
                }
                if (fixSuccessStatus) {
                    // fix file processor has added the fix code file
                    return checkFixResult(fixFileProcessor, rootDir, brokenApiUsage, brokenApiUsageList, null, codeFile, cacheDir);
                }
            }
        }

        return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                .brokenApiUsageList(brokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
    }

    private static void extractClientUsage(File fixProcessedFile, BrokenApiUsage brokenApiUsage,
                                           File clientJarFile, File codeFile) {
        Integer currentErrorLineNumber = brokenApiUsage.getErrorResult().getErrorLineNumber();
        Integer mappedLineNumber = CodeMappingUtil.mappingCodeWithLineNumber(fixProcessedFile, codeFile, currentErrorLineNumber);
        if (mappedLineNumber == -1) mappedLineNumber = currentErrorLineNumber;

        ApiSignature apiSignature = brokenApiUsage.getApiSignature();

        // still slicing in the original file, due to the client jar file can't be updated
        CodeSlicer.initSlicingEnvironment(clientJarFile);
        if (apiSignature == null) {
            System.out.println("NULL");
        }
        CodeSlicer codeSlicer = SlicingFactory.getCodeSlicer(codeFile, List.of(mappedLineNumber), clientJarFile, apiSignature);
        SlicingResult clientSlicingResult = codeSlicer.slicedCodeWithSyntaxCompletion();
        List<Integer> taintedLineNumbers = clientSlicingResult.getTaintedLineNumbers();
        String clientSlicedCode = clientSlicingResult.getSlicedCode();
        brokenApiUsage.setSlicedBrokenCode(clientSlicedCode);
        try {
            String brokenContent = BrokenApiContentExtractor.extractBrokenContent(codeFile, taintedLineNumbers);
            brokenApiUsage.setBrokenContent(brokenContent);
        } catch (BadLocationException e) {
            log.info("ClientBrokenAPIUsageExtractionError: ", e);
        }
    }


    // check the code file is java source code or java test case source code
    private static boolean checkIsSourceCode(File codeFile) {
        String codeFilePath = codeFile.getAbsolutePath();
        //TODO need to get the test directory in pom.xml
        return !codeFilePath.contains("src" + File.separator + "test");
    }

    /**
     * merge the previous code file and llm code file
     *
     * @param previousFile   The original code file to be repaired
     * @param promptCodeFile The code file providing prompts to the large language model for repair
     * @param llmCodeFile    The code file generated by the large language model after repair
     * @param cacheDir       The directory used for storing cache files
     * @param tempFilePrefix The prefix for cache file names
     * @return The fix code file
     */
    private static File mergeToFile(BrokenApiUsage brokenApiUsage, File previousFile, File promptCodeFile, File llmCodeFile,
                                    File cacheDir, String tempFilePrefix) {
        log.info("MergeCodeStart");
        CompilationUnit previousCompilationUnit = JDTUtil.parseCode(previousFile);
        CodeElementMapper codeElementMapper = new CodeElementMapper(promptCodeFile, llmCodeFile);
        PatchChecker patchChecker = new PatchChecker(brokenApiUsage, codeElementMapper);
        patchChecker.check();

        CodeMergeVisitor codeMergeVisitor = new CodeMergeVisitor(previousCompilationUnit, codeElementMapper);
        previousCompilationUnit.accept(codeMergeVisitor);
        // rewrite the source code
        Document doc = new Document(new FileReader(previousFile).readString());
        TextEdit edits = codeMergeVisitor.getMergeRewrite().rewriteAST(doc, null);
        try {
            edits.apply(doc);
            String fixCode = new CodeFormatter(doc.get()).startFormat();
            File fixCodeFile = FileUtil.createTempFile("Temp_" + tempFilePrefix + "_", ".java.cache", cacheDir, true);
            return new FileWriter(fixCodeFile).write(fixCode);
        } catch (Exception ignored) {
        }
        return null;
    }

    // return the fixed code file
    private static LLMFixResult callLLM(LLMFixModel llmFixModel, FixPromptGenerator fixPromptGenerator,
                                        BrokenApiUsage brokenApiUsage, File cacheDir) {
        log.info("CallLLMStart");
        String prompt;
        String answer;
        prompt = fixPromptGenerator.generate();
        try {
            answer = llmFixModel.call(prompt);
        } catch (Exception e) {
            answer = "";
        }

        String prefixFileName = FileNameUtil.getPrefix(brokenApiUsage.getErrorResult().getFilePath());
        File llmCodeFile = FileUtil.file(cacheDir, StrUtil.format(LLM_CODE_FILE_NAME, prefixFileName, IdUtil.getSnowflakeNextIdStr()));
        new FileWriter(llmCodeFile).write(new CodeFormatter(answer).startFormat());

        File promptFile = FileUtil.file(cacheDir, "prompt_" + prefixFileName + "_" + TOTAL_FIX_ROUND + ".txt");
        new FileWriter(promptFile).write(prompt);
        return new LLMFixResult(llmCodeFile, prompt, answer);
    }

    private static final Map<ApiSignature, JavaDocHint> JAVA_DOC_HINT_CACHE = new HashMap<>();

    private static JavaDocHint retrieveJavaDocHint(BrokenApiUsage brokenApiUsage, File libraryDatabaseDir) {
        log.info("RetrieveJavaDocHintStart");
        ApiSignature apiSignature = brokenApiUsage.getApiSignature();
        if (JAVA_DOC_HINT_CACHE.containsKey(apiSignature)) return JAVA_DOC_HINT_CACHE.get(apiSignature);

        JavadocFixHintProcessor javadocFixHintProcessor = new JavadocFixHintProcessor(brokenApiUsage, libraryDatabaseDir);
        JavaDocHint javaDocHint = javadocFixHintProcessor.extractHint();
        if (javaDocHint == null) return null;

        JAVA_DOC_HINT_CACHE.put(apiSignature, javaDocHint);
        return javaDocHint;
    }

    private static final Map<ApiSignature, MigrationCaseHint> MIGRATION_CASE_HINT_CACHE = new HashMap<>();

    private static MigrationCaseHint retrieveApiUsageChangeHit(BrokenApiUsage brokenApiUsage, File libraryDatabaseDir, File cacheDir) {
        log.info("RetrieveMigrationCaseHintStart");
        ApiSignature apiSignature = brokenApiUsage.getApiSignature();
        if (MIGRATION_CASE_HINT_CACHE.containsKey(apiSignature)) return MIGRATION_CASE_HINT_CACHE.get(apiSignature);

        SourceCodeFixHintProcessor sourceCodeFixHintProcessor = new SourceCodeFixHintProcessor(brokenApiUsage, libraryDatabaseDir,
                cacheDir, TOTAL_FIX_ROUND);
        MigrationCaseHint usageChangeHint = sourceCodeFixHintProcessor.extractHint();
        if (usageChangeHint == null) return null;

        MIGRATION_CASE_HINT_CACHE.put(apiSignature, usageChangeHint);
        return usageChangeHint;
    }
}
