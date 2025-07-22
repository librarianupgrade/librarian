package com.strange.fix.engine.fixer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.fix.engine.FixBrokenApiAnalyzer;
import com.strange.fix.engine.FixFileProcessor;
import com.strange.fix.engine.FixResult;
import com.strange.fix.engine.llm.LLMFixModel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public abstract class CodeFixer {

    public abstract FixResult fix(FixFileProcessor fixFileProcessor, LLMFixModel llmFixModel, File rootDir, File projectDir,
                                  BrokenApiUsage brokenApiUsage, List<BrokenApiUsage> brokenApiUsageList,
                                  File libraryDatabaseDir, Integer maxRetryCount, File cacheDir, int fixDepth);

    protected static FixResult checkFixResult(FixFileProcessor fixFileProcessor, File rootDir, BrokenApiUsage brokenApiUsage,
                                              List<BrokenApiUsage> oldBrokenApiUsageList, File fixCodeFile, File codeFile, File cacheDir) {
        log.info("CheckFixResultStart");
        FixBrokenApiAnalyzer fixBrokenApiAnalyzer = new FixBrokenApiAnalyzer(fixFileProcessor, rootDir, cacheDir,
                brokenApiUsage, oldBrokenApiUsageList, codeFile, fixCodeFile);
        FixResult fixResult = fixBrokenApiAnalyzer.analysisFixResult();
        switch (fixResult.getFixType()) {
            case NOT_FIXED -> log.info("Current Broken Api Usage is Not Fixed: {}", brokenApiUsage.getErrorResult().getCodeFile());
            case PARTIALLY_FIXED -> log.info("Current Broken Api Usage is Fixed");
            case FIXED_WITH_NEW_BROKEN_APIS -> log.info("Current Broken Api Usage is Fixed but Introducing Other Breaking: {}", brokenApiUsage.getErrorResult().getCodeFile());
            case FULLY_FIXED -> {
                log.info("All Broken Api Usages in Project are Fixed");
                new FileWriter(FileUtil.file(cacheDir, "success.log")).write("Success");
            }
        }
        return fixResult;
    }
}
