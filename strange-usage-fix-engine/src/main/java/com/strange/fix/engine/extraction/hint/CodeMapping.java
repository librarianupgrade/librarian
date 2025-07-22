package com.strange.fix.engine.extraction.hint;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class CodeMapping {
    @Alias("previous_code")
    private String previousCode;

    @Alias("refactored_code")
    private String refactoredCode;

    @Alias("similar_score")
    private Double SimilarityScore;

    public CodeMapping(String previousCode, String refactoredCode, Double SimilarityScore) {
        this.previousCode = previousCode;
        this.refactoredCode = refactoredCode;
        this.SimilarityScore = SimilarityScore;
    }
}
