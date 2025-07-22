package com.strange.fix.engine.slicing;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.common.utils.SootUtil;

import cn.hutool.core.collection.CollUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
public abstract class CodeSlicer {
    protected final File slicedFile; // target slicing file
    protected final List<Integer> lineNumberList; // slicing line numbers
    protected final File jarFile; // jar file path
    protected final ApiSignature apiSignature; // api signature


    public CodeSlicer( File slicedFile,  List<Integer> lineNumberList,  File jarFile,
                      ApiSignature apiSignature) {

        this.slicedFile = slicedFile;
        this.lineNumberList = lineNumberList;
        this.apiSignature = apiSignature;
        this.jarFile = jarFile;
    }

    public static void initSlicingEnvironment( File jarFile) {
        initSlicingEnvironment(List.of(jarFile));
    }

    public static void initSlicingEnvironment( List<File> jarFileList) {
        if(CollUtil.isEmpty(jarFileList)) {
            log.warn("SlicigEnvironmentIsEmpty");
        }
        SootUtil.initializeSoot(jarFileList);
    }

    public abstract SlicingResult slicedCodeWithoutSyntaxCompletion();

    public abstract SlicingResult slicedCodeWithSyntaxCompletion();

}
