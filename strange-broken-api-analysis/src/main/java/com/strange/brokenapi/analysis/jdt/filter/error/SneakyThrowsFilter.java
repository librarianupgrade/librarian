package com.strange.brokenapi.analysis.jdt.filter.error;

import com.strange.common.utils.JDTUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SneakyThrowsFilter extends BaseErrorFilter {

    private static final Integer UNHANDLED_EXCEPTION = 16777384;

    private static final String SNEAKY_THROWS_ANNOTATION_NAME = "lombok.SneakyThrows";

    public SneakyThrowsFilter(Map<String, File> javaCodeFileMap) {
        super(javaCodeFileMap);
    }

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        if (problem.getID() == UNHANDLED_EXCEPTION) {
            List<String> imports = JDTUtil.getImports(sourceCodeFile);
            return !imports.contains(SNEAKY_THROWS_ANNOTATION_NAME);
        }
        return true;
    }
}
