package com.strange.brokenapi.analysis.jdt.filter.error;

import com.strange.brokenapi.analysis.jdt.filter.ProblemFilter;
import com.strange.common.utils.JDTUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.List;
import java.util.Set;

public class LogFilter implements ProblemFilter {

    private static final String LOG_ANNOTATION_NAME = "lombok.extern.slf4j.Slf4j";
    private static final Set<String> LOG_GENERATE_SYMBOL = Set.of("log", "info", "error", "warn");

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        List<String> classAnnotations = JDTUtil.getClassAnnotations(sourceCodeFile);
        String[] arguments = problem.getArguments();

        // Remove the `log` from @Slf4j
        if (classAnnotations.contains(LOG_ANNOTATION_NAME)) {
            for (String argument : arguments) {
                if (LOG_GENERATE_SYMBOL.contains(argument)) {
                    return false;
                }
            }
        }

        return true;
    }
}
