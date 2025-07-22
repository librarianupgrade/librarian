package com.strange.fix.engine.extraction.javadoc.entity;

import com.strange.common.utils.ClassUtil;
import lombok.Data;
import org.apache.commons.lang3.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class ProjectDeprecation {

    private List<ClassDeprecation> classDeprecationList;

    private List<MethodDeprecation> methodDeprecationList;

    private List<FieldDeprecation> fieldDeprecationList;

    public ProjectDeprecation() {
        this.classDeprecationList = new ArrayList<>();
        this.methodDeprecationList = new ArrayList<>();
        this.fieldDeprecationList = new ArrayList<>();
    }

    public void addClassDeprecation(ClassDeprecation classDeprecation) {
        this.classDeprecationList.add(classDeprecation);
    }

    public void addMethodDeprecation(MethodDeprecation methodDeprecation) {
        this.methodDeprecationList.add(methodDeprecation);
    }

    public void addFieldDeprecation(FieldDeprecation fieldDeprecation) {
        this.fieldDeprecationList.add(fieldDeprecation);
    }

    public ClassDeprecation findClassDeprecation(String className) {
        for (ClassDeprecation classDeprecation : classDeprecationList) {
            if (classDeprecation.getOriginalClassName().equals(className)) {
                return classDeprecation;
            }
        }
        return null;
    }

    public MethodDeprecation findMethodDeprecation(String className, String methodName, List<String> paramList) {
        for (MethodDeprecation methodDeprecation : methodDeprecationList) {
            if (methodDeprecation.getOriginalMethodName().equals(methodName) && methodDeprecation.getOriginalClassName().equals(className)
                    && isEqualParameters(paramList, methodDeprecation.getOriginalMethodParameters())) {
                return methodDeprecation;
            }
        }
        return null;
    }

    private boolean isEqualParameters(List<String> actualParameters, List<String> exceptedParameters) {
        if (actualParameters.size() == exceptedParameters.size()) {
            for (int i = 0; i < actualParameters.size(); i++) {
                String actualClassName = ClassUtil.removeGenericType(actualParameters.get(i));
                String actualSimpleClassName = ClassUtil.getSimpleClassName(actualClassName);

                String exceptedClassName = ClassUtil.removeGenericType(exceptedParameters.get(i));
                String exceptedSimpleClassName = ClassUtil.getSimpleClassName(exceptedClassName);
                if (Objects.equals(actualSimpleClassName, exceptedSimpleClassName)) {
                    continue;
                }
                if (Objects.equals(actualSimpleClassName, "null")) {
                    continue;
                }
                if (checkAssignable(actualClassName, exceptedClassName)) {
                    continue;
                }
                if (!Objects.equals(actualSimpleClassName, exceptedSimpleClassName)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAssignable(String className, String toClassName) {
        try {
            Class<?> cls = ClassUtils.getClass(Thread.currentThread().getContextClassLoader(), className);
            Class<?> toClass = ClassUtils.getClass(Thread.currentThread().getContextClassLoader(), toClassName);
            return ClassUtils.isAssignable(cls, toClass);
        } catch (Throwable e) {
            return false;
        }
    }

    public FieldDeprecation findFieldDeprecation(String className, String fieldName) {
        for (FieldDeprecation fieldDeprecation : fieldDeprecationList) {
            if (fieldDeprecation.getOriginalFieldName().equals(fieldName) && fieldDeprecation.getOriginalClassName().equals(className)) {
                return fieldDeprecation;
            }
        }
        return null;
    }
}
