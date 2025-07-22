package com.strange.brokenapi.analysis.jdt.filter.deprecation;

import cn.hutool.core.util.ReUtil;
import com.strange.brokenapi.analysis.jdt.filter.ProblemFilter;
import com.strange.common.utils.ClassUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class DeprecationFilter implements ProblemFilter {
    private static final Integer METHOD_DEPRECATION = 67108967;

    private static final Integer CLASS_DEPRECATION = 16777221;

    private static final Integer CONSTRUCTOR_DEPRECATION = 134217861;

    private static final Integer FIELD_DEPRECATION = 33554505;

    private static final Set<String> IGNORE_DEPRECATION = Set.of("^java\\.math\\..*", "^java\\.util\\..*", "^java\\.security\\..*", "^java\\.lang\\..*",
            "^javax\\.security\\..*", "^java\\.net\\..*", "^java\\.io\\..*", "^java\\.awt\\..*");

    private final Map<String, File> javaCodeFileMap;

    public DeprecationFilter(Map<String, File> javaCodeFileMap) {
        this.javaCodeFileMap = javaCodeFileMap;
    }

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        if (problem.getID() == CLASS_DEPRECATION) {
            // Class Deprecation
            for (String argument : problem.getArguments()) {
                argument = ClassUtil.removeGenericType(argument);
                // skip client project's deprecation
                if (javaCodeFileMap.containsKey(argument)) {
                    return false;
                }
                for (String rule : IGNORE_DEPRECATION) {
                    if (ReUtil.isMatch(rule, argument)) {
                        return false;
                    }
                }
            }
            return true;
        } else if (problem.getID() == METHOD_DEPRECATION || problem.getID() == CONSTRUCTOR_DEPRECATION) {
            // Method or Constructor Deprecation
            String belongedClassName = problem.getArguments()[0];
            belongedClassName = ClassUtil.removeGenericType(belongedClassName);
            if (javaCodeFileMap.containsKey(belongedClassName)) {
                return false;
            }
            for (String rule : IGNORE_DEPRECATION) {
                if (ReUtil.isMatch(rule, belongedClassName)) {
                    return false;
                }
            }
            return true;
        } else if (problem.getID() == FIELD_DEPRECATION) {
            // Field Deprecation
            String belongedClassName = problem.getArguments()[0];
            belongedClassName = ClassUtil.removeGenericType(belongedClassName);
            if (javaCodeFileMap.containsKey(belongedClassName)) {
                return false;
            }
            for (String rule : IGNORE_DEPRECATION) {
                if (ReUtil.isMatch(rule, belongedClassName)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
