package com.strange.fix.engine;

import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.fix.engine.fixer.CodeFixer;
import com.strange.fix.engine.fixer.CodeFixerFactory;
import com.strange.fix.engine.llm.LLMFixModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public class FixEngine {
    public static FixResult fix( FixFileProcessor fixFileProcessor,  LLMFixModel llmFixModel,  File rootDir,  File projectDir,
                                 BrokenApiUsage brokenApiUsage,  List<BrokenApiUsage> brokenApiUsageList,
                                 File libraryDatabaseDir,  Integer maxRetryCount,  File cacheDir, int fixDepth) {

        CodeFixer codeFixer = CodeFixerFactory.getCodeFixer(brokenApiUsage);
        return codeFixer.fix(fixFileProcessor, llmFixModel, rootDir, projectDir, brokenApiUsage,
                brokenApiUsageList, libraryDatabaseDir, maxRetryCount, cacheDir, fixDepth);
    }
}
