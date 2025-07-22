package com.strange.fix.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import com.strange.brokenapi.analysis.BrokenApiExtractor;
import com.strange.brokenapi.analysis.BrokenApiUsage;
import com.strange.fix.engine.enums.FixEnum;
import lombok.NonNull;

import java.io.File;
import java.util.*;

public class FixBrokenApiAnalyzer {

    private final static Set<Integer> UNDEFINED_SYMBOL_SET = Set.of(16777218, 570425394);

    private final FixFileProcessor fixFileProcessor;

    private final File rootDir;

    private final File cacheDir;

    private final File codeFile;

    private final File fixCodeFile;

    private final BrokenApiUsage targetBrokenApiUsage;

    private final List<BrokenApiUsage> oldBrokenApiUsageList;

    public FixBrokenApiAnalyzer( FixFileProcessor fixFileProcessor,  File rootDir,  File cacheDir,  BrokenApiUsage targetBrokenApiUsage,
                                 List<BrokenApiUsage> oldBrokenApiUsageList, File codeFile, File fixCodeFile) {
        this.fixFileProcessor = fixFileProcessor;
        this.rootDir = rootDir;
        this.cacheDir = cacheDir;
        this.targetBrokenApiUsage = targetBrokenApiUsage;
        this.oldBrokenApiUsageList = oldBrokenApiUsageList;
        this.codeFile = codeFile;
        this.fixCodeFile = fixCodeFile;
    }


    public FixResult analysisFixResult() {
        // Write the files involved in the fix process into the project
        Map<String, String> originalFileContentMap = new HashMap<>();
        Map<String, File> tempFixFileMap = new HashMap<>(fixFileProcessor.getTempFixFileMap());

        // add the fix code file if is not null
        if (fixCodeFile != null) {
            tempFixFileMap.put(codeFile.getAbsolutePath(), fixCodeFile);
        }

        for (Map.Entry<String, File> entry : tempFixFileMap.entrySet()) {
            String filePath = entry.getKey();
            File fixFile = entry.getValue();
            String originalContent = new FileReader(filePath).readString();
            originalFileContentMap.put(filePath, originalContent);
            String fixContent = new FileReader(fixFile).readString();
            new FileWriter(filePath).write(fixContent);
        }

        // Analyze the broken API usages in the project during the current fix process
        List<BrokenApiUsage> fixBrokenApiUsageList = BrokenApiExtractor.extractBrokenApi(rootDir, cacheDir);

        // Write the original content back to the source file
        for (Map.Entry<String, String> entry : originalFileContentMap.entrySet()) {
            String filePath = entry.getKey();
            String originalContent = entry.getValue();
            new FileWriter(filePath).write(originalContent);
        }

        if (CollUtil.isEmpty(fixBrokenApiUsageList)) {
            return FixResult.builder().fixStatus(true).fixType(FixEnum.FULLY_FIXED)
                    .brokenApiUsageList(Collections.emptyList()).addedBrokenApiUsageList(Collections.emptyList()).build();
        }

        for (BrokenApiUsage brokenApiUsage : fixBrokenApiUsageList) {
            if (Objects.equals(brokenApiUsage, targetBrokenApiUsage)) {
                // it indicates the target brokenApiUsage is not fixed
                return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                        .brokenApiUsageList(oldBrokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
            }
        }

        // it indicates the target brokenApiUsage is fixed
        ArrayList<BrokenApiUsage> existedBrokenApiUsageList = new ArrayList<>();
        List<BrokenApiUsage> addedBrokenApiUsage = new ArrayList<>();

        for (BrokenApiUsage brokenApiUsage : fixBrokenApiUsageList) {
            BrokenApiUsage existedBrokenApiUsage = getFromExistedBrokenApiUsage(brokenApiUsage, oldBrokenApiUsageList);
            if (existedBrokenApiUsage != null) {
                existedBrokenApiUsage.setErrorResult(brokenApiUsage.getErrorResult()); // need to update the error result
                existedBrokenApiUsageList.add(existedBrokenApiUsage);
            } else {
                addedBrokenApiUsage.add(brokenApiUsage);
            }
        }

        // remove the undefined symbol added error
        for (BrokenApiUsage brokenApiUsage : addedBrokenApiUsage) {
            Integer errorId = brokenApiUsage.getErrorResult().getErrorId();
            if (UNDEFINED_SYMBOL_SET.contains(errorId)) {
                return FixResult.builder().fixStatus(false).fixType(FixEnum.NOT_FIXED)
                        .brokenApiUsageList(oldBrokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
            }
        }

        if (CollUtil.isEmpty(addedBrokenApiUsage)) {
            return FixResult.builder().fixStatus(true).fixType(FixEnum.PARTIALLY_FIXED)
                    .brokenApiUsageList(existedBrokenApiUsageList).addedBrokenApiUsageList(Collections.emptyList()).build();
        } else {
            return FixResult.builder().fixStatus(true).fixType(FixEnum.FIXED_WITH_NEW_BROKEN_APIS)
                    .brokenApiUsageList(existedBrokenApiUsageList).addedBrokenApiUsageList(addedBrokenApiUsage).build();
        }
    }

    private BrokenApiUsage getFromExistedBrokenApiUsage(BrokenApiUsage targetBrokenApiUsage, List<BrokenApiUsage> existedBrokenApiUsage) {
        for (BrokenApiUsage brokenApiUsage : existedBrokenApiUsage) {
            if (Objects.equals(brokenApiUsage, targetBrokenApiUsage)) {
                return brokenApiUsage;
            }
        }
        return null;
    }
}
