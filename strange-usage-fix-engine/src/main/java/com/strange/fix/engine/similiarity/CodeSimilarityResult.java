package com.strange.fix.engine.similiarity;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;

@Data
public class CodeSimilarityResult {

    private String code;

    @PropIgnore
    private String normalizedCode;

    private double similarityScore;

    public CodeSimilarityResult(String code, String normalizedCode, double similarityScore) {
        this.code = code;
        this.normalizedCode = normalizedCode;
        this.similarityScore = similarityScore;
    }
}
