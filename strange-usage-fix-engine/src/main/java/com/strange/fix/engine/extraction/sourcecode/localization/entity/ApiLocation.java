package com.strange.fix.engine.extraction.sourcecode.localization.entity;

import com.strange.brokenapi.analysis.ApiSignature;
import com.strange.brokenapi.analysis.enums.ApiTypeEnum;
import lombok.Data;

import java.io.File;

@Data
public abstract class ApiLocation {
    private ApiTypeEnum apiType;

    private File locatedFile;

    private Integer startLineNumber;

    private Integer endLineNumber;

    private ApiSignature locatedApiSignature;

    private ApiSignature targetApiSignature;
}
