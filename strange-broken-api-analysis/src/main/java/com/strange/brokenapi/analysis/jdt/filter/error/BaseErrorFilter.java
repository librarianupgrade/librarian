package com.strange.brokenapi.analysis.jdt.filter.error;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Pair;
import com.strange.brokenapi.analysis.jdt.filter.ProblemFilter;
import com.strange.common.utils.JDTUtil;
import org.eclipse.jdt.core.compiler.IProblem;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class BaseErrorFilter implements ProblemFilter {
    private final Map<String, File> javaCodeFileMap;

    public BaseErrorFilter(Map<String, File> javaCodeFileMap) {
        this.javaCodeFileMap = javaCodeFileMap;
    }

    @Override
    public abstract boolean filter(File sourceCodeFile, IProblem problem);

    protected String getFieldNameFromMethodName(List<JDTUtil.ClassField> fieldList, String methodName) {
        if (methodName.startsWith("is")) {
            String fieldName = methodName.substring(2);
            for (JDTUtil.ClassField classField : fieldList) {
                if (Objects.equals(classField.getFieldType(), "boolean") && classField.getFieldName().equalsIgnoreCase(fieldName)) {
                    return classField.getFieldName();
                }
            }
        } else if (methodName.startsWith("set") || methodName.startsWith("get")) {
            String fieldName = methodName.substring(3);
            for (JDTUtil.ClassField classField : fieldList) {
                if (classField.getFieldName().equalsIgnoreCase(fieldName)) {
                    return classField.getFieldName();
                }
            }
        }
        return null;
    }

    protected boolean validateMissedMethod(File sourceCodeFile, String missedMethodName, Set<String> annotationSet) {
        // @Getter, @Setter and @Data is in the class head
        List<String> annotations = JDTUtil.getClassAnnotations(sourceCodeFile);
        List<String> fieldSetterAndGetter = JDTUtil.getFieldSetterAndGetter(sourceCodeFile);

        for (String anno : annotationSet) {
            if (annotations.contains(anno)) {
                if (fieldSetterAndGetter.contains(missedMethodName)) {
                    return false;
                }
            }
        }

        // @Getter, @Setter and @Data is in the field head
        List<JDTUtil.ClassField> fields = JDTUtil.getFieldsInClass(sourceCodeFile);
        String fieldName = getFieldNameFromMethodName(fields, missedMethodName);
        if (fieldName != null) {
            List<String> fieldAnnotations = JDTUtil.getFieldAnnotations(sourceCodeFile, fieldName);
            for (String anno : annotationSet) {
                if (fieldAnnotations.contains(anno)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected Pair<String, String> isInnerClass(String className) {
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return null;
        }
        String rawClassName = className.substring(0, lastDotIndex);
        String innerClassName = className.substring(lastDotIndex + 1);
        if (javaCodeFileMap.containsKey(rawClassName)) {
            File file = javaCodeFileMap.get(rawClassName);
            if (JDTUtil.isInnerClass(file, innerClassName)) {
                return new Pair<>(rawClassName, innerClassName);
            } else {
                return null;
            }
        }
        return null;
    }

    protected File writeInnerClassToTempFile(Pair<String, String> classPair) {
        File file = javaCodeFileMap.get(classPair.getKey());
        String innerClassSourceCode = JDTUtil.getInnerClassSourceCode(file, classPair.getValue());
        File tempFile = FileUtil.createTempFile("tmp_source_code_", ".java", true);
        FileUtil.writeUtf8String(innerClassSourceCode, tempFile);
        return tempFile;
    }
}
