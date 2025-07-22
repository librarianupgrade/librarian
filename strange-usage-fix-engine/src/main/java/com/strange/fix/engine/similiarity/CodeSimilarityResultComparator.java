package com.strange.fix.engine.similiarity;

import java.util.Comparator;

public class CodeSimilarityResultComparator implements Comparator<CodeSimilarityResult> {
    @Override
    public int compare(CodeSimilarityResult o1, CodeSimilarityResult o2) {
        double score1 = o1.getSimilarityScore();
        double score2 = o2.getSimilarityScore();
        return Double.compare(score2, score1);
    }
}
