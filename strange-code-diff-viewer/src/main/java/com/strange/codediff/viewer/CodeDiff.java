package com.strange.codediff.viewer;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;

import java.util.Arrays;
import java.util.List;

public class CodeDiff {

    private static final String LABEL = "Diff";

    private static final Integer CONTEXT_SIZE = 10;

    private String previousCode;

    private String posteriorCode;

    public CodeDiff(String previousCode, String posteriorCode) {
        this.previousCode = previousCode;
        this.posteriorCode = posteriorCode;
    }

    public String getDiffString() throws DiffException {
        List<String> originalLines = Arrays.stream(previousCode.split("\n")).toList();
        Patch<String> patch = DiffUtils.diff(previousCode, posteriorCode, null);
        List<String> diffStringList = UnifiedDiffUtils.generateUnifiedDiff(LABEL, LABEL, originalLines, patch, CONTEXT_SIZE);
        return String.join("\n", diffStringList);
    }
}
