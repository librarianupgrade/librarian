package com.strange.fix.engine.extraction.hint;

import java.util.Comparator;

public class CodeMappingComparator implements Comparator<CodeMapping> {
    @Override
    public int compare(CodeMapping o1, CodeMapping o2) {
        double score1 = o1.getSimilarityScore();
        double score2 = o2.getSimilarityScore();
        return Double.compare(score2, score1);
    }
}
