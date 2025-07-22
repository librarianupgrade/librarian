package com.strange.brokenapi.analysis.jdt.filter.error;

import com.strange.common.utils.JDTUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnumFlagFilter extends BaseErrorFilter {
    private final static Integer UNRESOLVED_VARIABLE = 33554515;
    private final Map<String, File> javaCodeFileMap;
    private final Set<String> enumFlagSet;

    public EnumFlagFilter(Map<String, File> javaCodeFileMap) {
        super(javaCodeFileMap);
        this.javaCodeFileMap = javaCodeFileMap;
        this.enumFlagSet = new HashSet<>();
        initEnumFlagSet();
    }

    @Override
    public boolean filter(File sourceCodeFile, IProblem problem) {
        if (problem.getID() == UNRESOLVED_VARIABLE) {
            String[] arguments = problem.getArguments();
            String variableName = arguments[0];

            return !enumFlagSet.contains(variableName);
        }
        return true;
    }

    private void initEnumFlagSet() {
        for (File file : javaCodeFileMap.values()) {
            List<String> enumConstList = JDTUtil.getEnumConst(file);
            enumFlagSet.addAll(enumConstList);
        }
    }
}
