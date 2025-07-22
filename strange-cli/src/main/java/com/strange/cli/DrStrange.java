package com.strange.cli;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.strange.brokenapi.analysis.BrokenApiExtractor;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.brokenapi.analysis.InputProjectContext;
import com.strange.brokenapi.analysis.jdt.ErrorResult;
import com.strange.brokenapi.analysis.prioritization.BrokenApiContainer;
import com.strange.brokenapi.analysis.prioritization.BrokenApiPrioritizer;
import com.strange.cli.input.InputContext;
import com.strange.fix.engine.FixEngine;
import com.strange.fix.engine.FixFileProcessor;
import com.strange.fix.engine.FixResult;
import com.strange.fix.engine.llm.LLMFixModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Component
@Import(cn.hutool.extra.spring.SpringUtil.class)
@ComponentScan(basePackages = {"cn.hutool.extra.spring"})
public class DrStrange {

    private static final String ERROR_LOG_FILE_NAME = "error.log";

    public void run(InputContext inputContext) {
        FixFileProcessor fixFileProcessor = new FixFileProcessor();
        List<ErrorResult> fixedErrorList = new ArrayList<>();
        List<ErrorResult> unfixedErrorList = new ArrayList<>();

        try {
            // get the initial broken api usage in project
            List<BrokenApiUsage> brokenApiUsageList = BrokenApiExtractor.extractBrokenApi(inputContext.getProjectDir(), inputContext.getCacheDir());
            if (inputContext.isSkipFix()) return;
            InputProjectContext projectContext = new InputProjectContext(inputContext.getProjectDir());
            File rootDir = inputContext.getProjectDir();
            File projectDir = projectContext.getProjectRootDir();


            System.setErr(new PrintStream(OutputStream.nullOutputStream()));
            Set<BrokenApiUsage> hasTryFixedBrokenApiUsageSet = new HashSet<>();
            BrokenApiPrioritizer prioritizer = new BrokenApiPrioritizer(projectDir, brokenApiUsageList);
            Queue<BrokenApiContainer> brokenApiContainerQueue = prioritizer.prioritize();


            BrokenApiUsage targetBrokenApiUsage;
            // start the fix loop
            while ((targetBrokenApiUsage = getTargetBrokenApiUsage(brokenApiContainerQueue, hasTryFixedBrokenApiUsageSet)) != null) {
                // start fix a single broken api usage
                LLMFixModel llmFixModel = new LLMFixModel();
                FixResult fixResult;
                FixFileProcessor clonedFixFileProcessor = fixFileProcessor.clone();
                try {
                    fixResult = FixEngine.fix(clonedFixFileProcessor, llmFixModel, rootDir, projectDir, targetBrokenApiUsage, brokenApiUsageList,
                            inputContext.getLibraryDatabaseDir(), inputContext.getMaxRetryCount(), inputContext.getCacheDir(), 0);

                    if (fixResult.isFixed()) {
                        // TODO need to merge the processor
                        fixFileProcessor.setTempFixFileMap(clonedFixFileProcessor.getTempFixFileMap());
                        fixedErrorList.add(targetBrokenApiUsage.getErrorResult());
                        brokenApiUsageList = fixResult.getBrokenApiUsageList();
                        prioritizer = new BrokenApiPrioritizer(projectDir, brokenApiUsageList);
                        brokenApiContainerQueue = prioritizer.prioritize();
                    } else {
                        unfixedErrorList.add(targetBrokenApiUsage.getErrorResult());
                    }
                    hasTryFixedBrokenApiUsageSet.add(targetBrokenApiUsage);
                } catch (Throwable e) {
                    resolveError(inputContext, e);
                    hasTryFixedBrokenApiUsageSet.add(targetBrokenApiUsage);
                    unfixedErrorList.add(targetBrokenApiUsage.getErrorResult());
                }
            }
        } catch (Throwable ignored) {
        } finally {
            fixFileProcessor.applyFixChange();
            saveRunningData(inputContext.getCacheDir(), fixedErrorList, unfixedErrorList);
            log.info("Fix Broken Api Usage Finish");
        }
        System.exit(0);
    }

    private void saveRunningData(File cacheDir, List<ErrorResult> fixedErrorList, List<ErrorResult> unfixedErrorList) {
        File fixErrorFile = FileUtil.file(cacheDir, "fix_error_list.json");
        File unfixErrorFile = FileUtil.file(cacheDir, "unfix_error_list.json");
        new cn.hutool.core.io.file.FileWriter(fixErrorFile, Charset.defaultCharset()).write(JSONUtil.toJsonPrettyStr(fixedErrorList));
        new cn.hutool.core.io.file.FileWriter(unfixErrorFile, Charset.defaultCharset()).write(JSONUtil.toJsonPrettyStr(unfixedErrorList));
    }

    private BrokenApiUsage getTargetBrokenApiUsage(Queue<BrokenApiContainer> apiContainerQueue, Set<BrokenApiUsage> hasTryFixedBrokenApiUsageSet) {
        if (apiContainerQueue.isEmpty()) return null;
        while (!apiContainerQueue.isEmpty()) {
            BrokenApiContainer brokenApiContainer = apiContainerQueue.poll();
            for (BrokenApiUsage brokenApiUsage : brokenApiContainer) {
                if (!hasTryFixedBrokenApiUsageSet.contains(brokenApiUsage)) return brokenApiUsage;
            }
        }
        return null;
    }

    private void resolveError(InputContext inputContext, Throwable e) {
        log.error("DrStrangeFixError: ", e);
        PrintWriter printWriter = null;
        try {
            File cacheDir = inputContext.getCacheDir();
            File errorLogFile = FileUtil.file(cacheDir, ERROR_LOG_FILE_NAME);
            FileWriter fileWriter = new FileWriter(errorLogFile, true);
            printWriter = new PrintWriter(fileWriter);
            e.printStackTrace(printWriter);
        } catch (IOException ignored) {
        } finally {
            if (printWriter != null) printWriter.close();
        }
    }

    private String getOrdinalSuffix(int number) {
        int mod100 = number % 100;
        if (mod100 >= 11 && mod100 <= 13) return "th";
        return switch (number % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
