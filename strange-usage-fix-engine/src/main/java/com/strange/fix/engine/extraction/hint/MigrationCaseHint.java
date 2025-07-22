package com.strange.fix.engine.extraction.hint;

import com.strange.brokenapi.analysis.ApiSignature;
import lombok.Data;

import java.util.List;

@Data
public class MigrationCaseHint {
    private ApiSignature mappedApiSignature;

    private String clientSlicedCode;

    private List<CodeMapping> codeMappingList;
}
