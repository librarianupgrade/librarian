package com.strange.brokenapi.analysis.jdt.filter.error;

import cn.hutool.core.io.FileUtil;
import com.strange.common.utils.ClassUtil;
import com.strange.common.utils.JDTUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OtherFilter extends BaseErrorFilter {
    private final static Integer MAX_VALIDATE_DEPTH = 2;

    private final Map<String, File> javaCodeFileMap;

    public OtherFilter(Map<String, File> javaCodeFileMap) {
        super(javaCodeFileMap);
        this.javaCodeFileMap = javaCodeFileMap;
    }

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        String[] arguments = problem.getArguments();

        if (problem.getID() == INFER_ELIDED_TYPES_ERROR) return false;

        if (problem.getID() == HIERARCHY_ERROR) return false;

        if (problem.getID() == UNINITIALIZED_FINAL_FIELD) return false;

        if (problem.getID() == INFER_INVOCATION_TYPE_ERROR) return false;

        if (problem.getID() == DANGLING_REFERENCE_ERROR) return false;

        if (problem.getID() == TARGET_NOT_FUNCTIONAL_INTERFACE) return false;

        if (problem.getID() == UNCLASSIFIED) return false;

        // Remove unrelated file
        if (IGNORED_FILE_NAMES.contains(FileUtil.getName(sourceCodeFile))) return false;

        // the class automated generated by framework
        if (arguments.length == 1) {
            String simpleClassName = ClassUtil.getSimpleClassName(arguments[0]);
            if (simpleClassName.length() >= 2 && simpleClassName.startsWith("Q")
                    && Character.isUpperCase(simpleClassName.charAt(1))) {
                return false;
            }
        }

        //  Remove JDK-related errors
        if (!validateBlacklist(sourceCodeFile, problem, 0)) return false;

        return true;
    }

    private boolean validateBlacklist(File sourceCodeFile, IProblem problem, Integer depth) {
        if (depth > MAX_VALIDATE_DEPTH) {
            return true;
        }

        String[] arguments = problem.getArguments();
        Map<String, String> importMap = JDTUtil.getImportMap(sourceCodeFile);

        for (String argument : arguments) {
            for (String ignoredClassName : BLACK_LIST) {
                if (argument.equals(ignoredClassName)) {
                    return false;
                }
            }
            if (importMap.containsKey(argument)) {
                String className = importMap.get(argument);
                for (String ignoredClassName : BLACK_LIST) {
                    if (className.equals(ignoredClassName)) {
                        return false;
                    }
                }
            }
        }

        List<String> superClassList = JDTUtil.getSuperClassAndInterfaces(sourceCodeFile);
        for (String superClassName : superClassList) {
            superClassName = ClassUtil.removeGenericType(superClassName);
            if (javaCodeFileMap.containsKey(superClassName)) {
                if (!validateBlacklist(javaCodeFileMap.get(superClassName), problem, depth + 1)) {
                    return false;
                }
            }
        }

        return true;
    }
}
