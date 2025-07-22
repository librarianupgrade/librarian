package com.strange.fix.engine.extraction.sourcecode;

import com.strange.fix.engine.extraction.hint.CodeMapping;
import com.strange.fix.engine.similiarity.CodeSimilarityResult;
import lombok.Data;

import java.util.List;

@Data
public class RetrieveCodeResult {
    private List<CodeMapping> codeMappingList;

    private List<CodeSimilarityResult> oldLibraryCodeSimilarityList;
}
